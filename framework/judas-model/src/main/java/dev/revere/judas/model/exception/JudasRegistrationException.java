package dev.revere.judas.model.exception;

/**
 * Base runtime exception for command registration failures.
 */
public class JudasRegistrationException extends RuntimeException {

    /**
     * @param message failure reason
     */
    public JudasRegistrationException(String message) {
        super(message);
    }

    /**
     * @param message failure reason
     * @param cause underlying cause
     */
    public JudasRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
