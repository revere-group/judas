package dev.revere.judas.model.exception;

/**
 * Thrown when a subcommand shortcut alias ({@code @RootCommand} on a {@code @Subcommand} method)
 * collides with an existing primary/root command alias.
 */
public class SubcommandShortcutAliasConflictException extends JudasRegistrationException {

    /**
     * @param message failure reason
     */
    public SubcommandShortcutAliasConflictException(String message) {
        super(message);
    }
}
