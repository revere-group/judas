package dev.revere.judas.model.exception;

/**
 * Thrown when an API call references a root command that is not registered.
 */
public class UnknownRootCommandException extends JudasRegistrationException {

    /**
     * @param message failure reason
     */
    public UnknownRootCommandException(String message) {
        super(message);
    }
}
