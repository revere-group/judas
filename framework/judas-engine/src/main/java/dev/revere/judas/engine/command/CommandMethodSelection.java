package dev.revere.judas.engine.command;

import dev.revere.judas.model.command.CommandMethodDescriptor;

/**
 * Outcome of resolving which annotated handler should run for the current input.
 */
public final class CommandMethodSelection {
    private final Kind kind;
    private final CommandMethodDescriptor method;
    private final String unknownToken;

    private CommandMethodSelection(Kind kind, CommandMethodDescriptor method, String unknownToken) {
        this.kind = kind;
        this.method = method;
        this.unknownToken = unknownToken;
    }

    /**
     * @param method matched handler
     * @return successful selection
     */
    public static CommandMethodSelection matched(CommandMethodDescriptor method) {
        return new CommandMethodSelection(Kind.MATCHED, method, null);
    }

    /**
     * @param token unknown subcommand token from user input
     * @return unknown-subcommand selection
     */
    public static CommandMethodSelection unknownSubcommand(String token) {
        return new CommandMethodSelection(Kind.UNKNOWN_SUBCOMMAND, null, token);
    }

    /**
     * @return selection indicating no callable handler is available
     */
    public static CommandMethodSelection noHandler() {
        return new CommandMethodSelection(Kind.NO_HANDLER, null, null);
    }

    /**
     * @return kind of routing result
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * @return matched method when {@link #getKind()} is {@link Kind#MATCHED}
     */
    public CommandMethodDescriptor getMethod() {
        return method;
    }

    /**
     * @return unknown token when {@link #getKind()} is {@link Kind#UNKNOWN_SUBCOMMAND}
     */
    public String getUnknownToken() {
        return unknownToken;
    }

    /**
     * High-level routing outcomes produced by {@link CommandMethodSelector}.
     */
    public enum Kind {
        MATCHED,
        UNKNOWN_SUBCOMMAND,
        NO_HANDLER
    }
}
