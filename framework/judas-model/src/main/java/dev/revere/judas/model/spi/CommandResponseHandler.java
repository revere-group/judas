package dev.revere.judas.model.spi;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

/**
 * Handles non-void return values from command handlers.
 */
public interface CommandResponseHandler {
    /**
     * Indicates whether this handler can process a specific command return value.
     *
     * @param response method return value
     * @return {@code true} when this handler can process the response
     */
    boolean supports(Object response);

    /**
     * Handles one supported command response value.
     *
     * <p>The runtime calls handlers in registration order and stops at the first handler
     * whose {@link #supports(Object)} returned {@code true}.
     *
     * @param context command context
     * @param root root descriptor
     * @param method handler descriptor
     * @param response method return value
     */
    void handle(CommandContext context, CommandDescriptor root, CommandMethodDescriptor method, Object response);
}
