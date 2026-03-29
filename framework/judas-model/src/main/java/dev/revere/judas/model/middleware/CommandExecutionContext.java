package dev.revere.judas.model.middleware;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.resolver.ArgumentBuffer;

/**
 * Context passed through command middleware chain.
 */
public final class CommandExecutionContext {
    private final CommandDescriptor root;
    private final CommandMethodDescriptor handler;
    private final CommandContext commandContext;
    private final ArgumentBuffer arguments;

    /**
     * Creates immutable middleware context for one handler invocation.
     *
     * @param root root descriptor
     * @param handler selected handler descriptor
     * @param commandContext command context
     * @param arguments mutable argument buffer
     */
    public CommandExecutionContext(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            CommandContext commandContext,
            ArgumentBuffer arguments
    ) {
        this.root = root;
        this.handler = handler;
        this.commandContext = commandContext;
        this.arguments = arguments;
    }

    /**
     * Returns the root descriptor currently being executed.
     *
     * @return root command descriptor
     */
    public CommandDescriptor getRoot() {
        return root;
    }

    /**
     * Returns the handler descriptor selected by routing.
     *
     * @return selected method descriptor
     */
    public CommandMethodDescriptor getHandler() {
        return handler;
    }

    /**
     * Returns runtime invocation context for sender and messaging operations.
     *
     * @return command execution context
     */
    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * Returns the mutable argument buffer shared across middleware and invocation stages.
     *
     * @return mutable command argument buffer
     */
    public ArgumentBuffer getArguments() {
        return arguments;
    }
}
