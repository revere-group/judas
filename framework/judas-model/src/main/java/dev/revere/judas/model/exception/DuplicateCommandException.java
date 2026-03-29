package dev.revere.judas.model.exception;

/**
 * Thrown when a root command alias or subcommand alias is already registered.
 */
public class DuplicateCommandException extends JudasRegistrationException {

    /**
     * @param message failure reason
     */
    public DuplicateCommandException(String message) {
        super(message);
    }
}
