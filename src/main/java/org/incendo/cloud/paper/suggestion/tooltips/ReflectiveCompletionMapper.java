package org.incendo.cloud.paper.suggestion.tooltips;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import java.lang.reflect.Method;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.bukkit.internal.CraftBukkitReflection;

final class ReflectiveCompletionMapper implements CompletionMapper {

    private final CompletionMapper wrapped;

    ReflectiveCompletionMapper() {
        if (CraftBukkitReflection.classExists("io.papermc.paper.command.brigadier.MessageComponentSerializer")) {
            this.wrapped = new Modern();
        } else {
            this.wrapped = new Legacy();
        }
    }

    @Override
    public AsyncTabCompleteEvent.@NonNull Completion map(final @NonNull TooltipSuggestion suggestion) {
        return this.wrapped.map(suggestion);
    }

    @SuppressWarnings("UnstableApiUsage")
    private static final class Modern implements CompletionMapper {
        private final Object serializer;
        private final Method deserializeOrNull;
        private final Method completionWithTooltipMethod;

        Modern() {
            final Method instance = CraftBukkitReflection.needMethod(
                MessageComponentSerializer.class,
                "message"
            );
            try {
                this.serializer = instance.invoke(null);
                this.deserializeOrNull = CraftBukkitReflection.needMethod(
                    MessageComponentSerializer.class,
                    "deserializeOrNull",
                    Object.class
                );
                this.completionWithTooltipMethod = CraftBukkitReflection.needMethod(
                    AsyncTabCompleteEvent.Completion.class,
                    "completion",
                    String.class,
                    this.deserializeOrNull.getReturnType()
                );
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AsyncTabCompleteEvent.@NonNull Completion map(final TooltipSuggestion suggestion) {
            final Message tooltip = suggestion.tooltip();
            if (tooltip == null) {
                return AsyncTabCompleteEvent.Completion.completion(suggestion.suggestion());
            }
            try {
                return (AsyncTabCompleteEvent.Completion) this.completionWithTooltipMethod.invoke(
                    null,
                    suggestion.suggestion(),
                    this.deserializeOrNull.invoke(this.serializer, tooltip)
                );
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("removal")
    private static final class Legacy implements CompletionMapper {
        private final java.lang.invoke.MethodHandle completionWithTooltip;
        private final java.lang.invoke.MethodHandle componentFromMessage;

        Legacy() {
            final Method componentFromMessageMethod = CraftBukkitReflection.needMethod(
                io.papermc.paper.brigadier.PaperBrigadier.class,
                "componentFromMessage",
                Message.class
            );
            final Method completionWithTooltipMethod = CraftBukkitReflection.needMethod(
                AsyncTabCompleteEvent.Completion.class,
                "completion",
                String.class,
                componentFromMessageMethod.getReturnType()
            );
            try {
                this.componentFromMessage = java.lang.invoke.MethodHandles.publicLookup().unreflect(componentFromMessageMethod);
                this.completionWithTooltip = java.lang.invoke.MethodHandles.publicLookup().unreflect(completionWithTooltipMethod);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AsyncTabCompleteEvent.@NonNull Completion map(final TooltipSuggestion suggestion) {
            final Message tooltip = suggestion.tooltip();
            if (tooltip == null) {
                return AsyncTabCompleteEvent.Completion.completion(suggestion.suggestion());
            }
            try {
                final Object component = this.componentFromMessage.invoke(tooltip);
                return (AsyncTabCompleteEvent.Completion) this.completionWithTooltip.invoke(suggestion.suggestion(), component);
            } catch (final Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
