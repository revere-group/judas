package dev.revere.judas.engine.command.routing;

import dev.revere.judas.model.command.CommandMethodDescriptor;

/**
 * Outcome of resolving which annotated handler should run for the current input.
 */
public final class CommandMethodSelection {
    private final Kind kind;
    private final CommandMethodDescriptor method;
    private final String unknownToken;

    /**
     * Creates a method-selection outcome.
     *
     * @param kind selection kind
     * @param method resolved method when matched
     * @param unknownToken unknown subcommand token when unresolved
     */
    private CommandMethodSelection(Kind kind, CommandMethodDescriptor method, String unknownToken) {
        this.kind = kind;
        this.method = method;
        this.unknownToken = unknownToken;
    }

    /**
     * Creates a successful routing result that points to an executable handler.
     *
     * @param method matched handler descriptor chosen by the selector
     * @return selection whose kind is {@link Kind#MATCHED}
     */
    public static CommandMethodSelection matched(CommandMethodDescriptor method) {
        return new CommandMethodSelection(Kind.MATCHED, method, null);
    }

    /**
     * Creates a routing result for an unknown first-token subcommand.
     *
     * <p>This outcome is used by the router to emit an "unknown subcommand" message that can include
     * available alternatives for the root.
     *
     * @param token unknown subcommand token from user input
     * @return selection whose kind is {@link Kind#UNKNOWN_SUBCOMMAND}
     */
    public static CommandMethodSelection unknownSubcommand(String token) {
        return new CommandMethodSelection(Kind.UNKNOWN_SUBCOMMAND, null, token);
    }

    /**
     * Creates a routing result indicating that no handler can run.
     *
     * <p>This can happen when a root defines no default method and no subcommand token was provided.
     *
     * @return selection whose kind is {@link Kind#NO_HANDLER}
     */
    public static CommandMethodSelection noHandler() {
        return new CommandMethodSelection(Kind.NO_HANDLER, null, null);
    }

    /**
     * Returns the high-level routing outcome kind.
     *
     * @return routing result kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the resolved handler for matched outcomes.
     *
     * @return matched method when {@link #getKind()} is {@link Kind#MATCHED}, otherwise {@code null}
     */
    public CommandMethodDescriptor getMethod() {
        return method;
    }

    /**
     * Returns the unresolved subcommand token for unknown-subcommand outcomes.
     *
     * @return unknown token when {@link #getKind()} is {@link Kind#UNKNOWN_SUBCOMMAND}, otherwise {@code null}
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
