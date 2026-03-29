package dev.revere.judas.engine.command.execution;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.spi.CommandExecutionServices;
import dev.revere.judas.model.spi.CommandResponseHandler;

/**
 * Dispatches non-void command responses to the first supporting response handler.
 */
public final class CommandResponseDispatcher {
    private final CommandExecutionServices executionServices;

    /**
     * Creates a dispatcher that delegates responses to registered response handlers.
     *
     * @param executionServices runtime response services
     */
    public CommandResponseDispatcher(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Dispatches one handler return value to the first supporting response handler.
     *
     * @param context execution context
     * @param descriptor root descriptor
     * @param methodDescriptor handler descriptor
     * @param response return value from handler
     */
    public void dispatch(
            CommandContext context,
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            Object response
    ) {
        if (response == null) {
            return;
        }
        for (CommandResponseHandler handler : this.executionServices.getResponseHandlers()) {
            if (!handler.supports(response)) {
                continue;
            }
            handler.handle(context, descriptor, methodDescriptor, response);
            return;
        }
    }
}
