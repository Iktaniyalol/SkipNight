package org.incendo.cloud.bukkit.parser;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.data.ProtoItemStack;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.component.CommandComponent.Builder;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.SuggestionProvider;

public class ItemStackParser<C>
        implements ArgumentParser.FutureArgumentParser<C, ProtoItemStack> {
    private final ArgumentParser<C, ProtoItemStack> parser = new LegacyParser();

    public static <C> @NonNull ParserDescriptor<C, ProtoItemStack> itemStackParser() {
        return ParserDescriptor.of(new ItemStackParser<C>(), ProtoItemStack.class);
    }

    public static <C> @NonNull Builder<C, ProtoItemStack> itemStackComponent() {
        return CommandComponent.<C, ProtoItemStack>builder().parser(itemStackParser());
    }

    public final @NonNull CompletableFuture<@NonNull ArgumentParseResult<ProtoItemStack>> parseFuture(
            @NonNull CommandContext<C> commandContext, @NonNull CommandInput commandInput
    ) {
        return this.parser.parseFuture(commandContext, commandInput);
    }

    public final @NonNull SuggestionProvider<C> suggestionProvider() {
        return this.parser.suggestionProvider();
    }

    private static final class LegacyParser<C>
            implements ArgumentParser.FutureArgumentParser<C, ProtoItemStack>,
            BlockingSuggestionProvider.Strings<C> {
        private final ArgumentParser<C, ProtoItemStack> parser = new MaterialParser().mapSuccess((ctx, material) ->
                CompletableFuture.completedFuture(new LegacyProtoItemStack((Material) material))
        );

        LegacyParser() {
        }

        @Override
        public @NonNull CompletableFuture<@NonNull ArgumentParseResult<ProtoItemStack>> parseFuture(
                @NonNull CommandContext<C> commandContext, @NonNull CommandInput commandInput
        ) {
            return this.parser.parseFuture(commandContext, commandInput);
        }

        @Override
        public @NonNull Iterable<String> stringSuggestions(
                @NonNull CommandContext<C> commandContext, @NonNull CommandInput input
        ) {
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(value -> value.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList());
        }

        private static final class LegacyProtoItemStack implements ProtoItemStack {
            private final Material material;

            LegacyProtoItemStack(@NonNull Material material) {
                this.material = material;
            }

            @Override
            public @NonNull Material material() {
                return this.material;
            }

            @Override
            public boolean hasExtraData() {
                return false;
            }

            @Override
            public @NonNull ItemStack createItemStack(int stackSize, boolean respectMaximumStackSize)
                    throws IllegalArgumentException {
                if (respectMaximumStackSize && stackSize > this.material.getMaxStackSize()) {
                    throw new IllegalArgumentException(String.format("The maximum stack size for %s is %d",
                            this.material, this.material.getMaxStackSize()));
                }
                return new ItemStack(this.material, stackSize);
            }
        }
    }
}
