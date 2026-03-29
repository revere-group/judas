package dev.revere.judas.model.exception;

/**
 * Thrown when a subcommand explicitly targets a different parent than requested.
 */
public class SubcommandParentMismatchException extends JudasRegistrationException {

    /**
     * @param message failure reason
     */
    public SubcommandParentMismatchException(String message) {
        super(message);
    }
}
