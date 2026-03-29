package dev.revere.judas.model.middleware;

/**
 * Continuation chain for command middleware execution.
 */
@FunctionalInterface
public interface CommandExecutionChain {

    /**
     * Continue execution with the next middleware or terminal handler invocation.
     */
    void proceed();
}
