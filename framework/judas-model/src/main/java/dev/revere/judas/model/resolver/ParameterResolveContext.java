package dev.revere.judas.model.resolver;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.ParameterDescriptor;

/**
 * Context data used while resolving a command parameter.
 */
public class ParameterResolveContext {
    private final CommandContext commandContext;
    private final ParameterDescriptor parameter;
    private final ArgumentBuffer arguments;

    /**
     * @param commandContext current command execution context
     * @param parameter parameter being resolved
     * @param arguments mutable argument buffer
     */
    public ParameterResolveContext(CommandContext commandContext, ParameterDescriptor parameter, ArgumentBuffer arguments) {
        this.commandContext = commandContext;
        this.parameter = parameter;
        this.arguments = arguments;
    }

    /**
     * @return current command execution context
     */
    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * @return parameter currently being resolved
     */
    public ParameterDescriptor getParameter() {
        return parameter;
    }

    /**
     * @return mutable argument buffer used for token consumption
     */
    public ArgumentBuffer getArguments() {
        return arguments;
    }
}
