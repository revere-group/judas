package dev.revere.judas.engine.command.execution;

import dev.revere.judas.api.annotation.CooldownScope;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.CooldownDefinition;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.Locale;

/**
 * Evaluates command cooldown state and computes stable cooldown storage keys.
 *
 * <p>The evaluator is intentionally deterministic:
 * the same invocation scope ({@link CooldownScope} + root + handler + sender + custom key) always maps to
 * the same cooldown key string, which allows interchangeable cooldown backends.
 */
public final class CommandCooldownEvaluator {
    private final CommandExecutionServices executionServices;

    /**
     * Creates a cooldown evaluator backed by runtime execution services.
     *
     * @param executionServices runtime service container used to access the configured cooldown service
     */
    public CommandCooldownEvaluator(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Attempts to acquire the command cooldown window and returns remaining time when blocked.
     *
     * <p>If no cooldown is configured for the handler (or the resolved cooldown duration is non-positive),
     * this method returns {@code 0} and performs no backend lookup.
     *
     * @param descriptor matched root command descriptor
     * @param methodDescriptor matched handler descriptor
     * @param context command execution context
     * @return remaining cooldown in milliseconds, or {@code 0} when invocation is allowed
     */
    public long acquireRemaining(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, CommandContext context) {
        CooldownDefinition cooldown = methodDescriptor.getCooldown();
        if (cooldown == null || cooldown.getDurationMillis() <= 0L) {
            return 0L;
        }
        String key = this.cooldownKey(descriptor, methodDescriptor, context, cooldown);
        return this.executionServices.getCooldownService().acquireOrGetRemaining(key, cooldown.getDurationMillis());
    }

    /**
     * Computes the canonical cooldown key for one invocation.
     *
     * <p>Key layout by scope:
     * <ul>
     *     <li>{@link CooldownScope#GLOBAL}: {@code cooldown:global}</li>
     *     <li>{@link CooldownScope#ROOT}: {@code cooldown:root:&lt;root&gt;}</li>
     *     <li>{@link CooldownScope#METHOD}: {@code cooldown:method:&lt;root&gt;:&lt;method&gt;}</li>
     *     <li>{@link CooldownScope#SENDER}: {@code cooldown:sender:&lt;sender&gt;:&lt;root&gt;:&lt;method&gt;}</li>
     * </ul>
     *
     * <p>When sender name is unavailable for sender-scoped cooldowns, an identity-based fallback token is used.
     * If a custom cooldown key suffix is configured, it is appended as {@code :&lt;custom&gt;}.
     *
     * @param descriptor matched root command descriptor
     * @param methodDescriptor matched handler descriptor
     * @param context command execution context
     * @param cooldown resolved cooldown metadata
     * @return canonical cooldown key for backend storage and lookup
     */
    private String cooldownKey(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            CooldownDefinition cooldown
    ) {
        String root = descriptor.getNames().length == 0 ? "root" : descriptor.getNames()[0].toLowerCase(Locale.ROOT);
        String method = methodDescriptor.getNames().length == 0
                ? methodDescriptor.getMethod().getName().toLowerCase(Locale.ROOT)
                : methodDescriptor.getNames()[0].toLowerCase(Locale.ROOT);
        String base;
        if (cooldown.getScope() == CooldownScope.GLOBAL) {
            base = "cooldown:global";
        } else if (cooldown.getScope() == CooldownScope.ROOT) {
            base = "cooldown:root:" + root;
        } else if (cooldown.getScope() == CooldownScope.METHOD) {
            base = "cooldown:method:" + root + ":" + method;
        } else {
            String sender = context.getSenderName();
            if (sender == null || sender.trim().isEmpty()) {
                Object senderObject = context.getSender();
                sender = senderObject == null ? "unknown" : Integer.toHexString(System.identityHashCode(senderObject));
            }
            base = "cooldown:sender:" + sender.toLowerCase(Locale.ROOT) + ":" + root + ":" + method;
        }
        if (cooldown.getKey().isEmpty()) {
            return base;
        }
        return base + ":" + cooldown.getKey();
    }
}
