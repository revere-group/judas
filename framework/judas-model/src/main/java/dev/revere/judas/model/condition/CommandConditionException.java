package dev.revere.judas.model.condition;

/**
 * Raised when a command condition rejects execution.
 */
public class CommandConditionException extends RuntimeException {

    /**
     * Creates a condition failure exception with a user-facing message.
     *
     * @param message condition failure message
     */
    public CommandConditionException(String message) {
        super(message);
    }
}
