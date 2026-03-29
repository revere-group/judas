package dev.revere.judas.model.middleware;

/**
 * Around-invocation middleware hook for command execution.
 */
@FunctionalInterface
public interface CommandMiddleware {

    /**
     * Executes middleware logic around command invocation.
     *
     * <p>Implementations may run code before and/or after delegating to {@code chain.proceed()}.
     *
     * @param context current execution context
     * @param chain continuation chain
     */
    void handle(CommandExecutionContext context, CommandExecutionChain chain);
}
