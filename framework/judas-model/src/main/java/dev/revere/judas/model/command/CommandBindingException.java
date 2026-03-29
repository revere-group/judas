package dev.revere.judas.model.command;

/**
 * Raised when command arguments cannot be bound to method parameters.
 */
public class CommandBindingException extends RuntimeException {

    /**
     * @param message failure reason
     */
    public CommandBindingException(String message) {
        super(message);
    }
}
