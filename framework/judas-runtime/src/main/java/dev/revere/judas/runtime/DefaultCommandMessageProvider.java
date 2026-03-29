package dev.revere.judas.runtime;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.spi.CommandMessageProvider;

/**
 * Default framework feedback messages.
 */
public final class DefaultCommandMessageProvider implements CommandMessageProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public String noPermissionForRoot(CommandDescriptor descriptor) {
        return "You do not have permission to use this command.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String noPermissionForSubcommand(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor) {
        return "You do not have permission to use this subcommand.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
        String suffix = availableSubcommands == null || availableSubcommands.isEmpty()
                ? ""
                : " Available: " + availableSubcommands + ".";
        return "Unknown subcommand '" + token + "'." + suffix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
        if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            return "Usage: /" + descriptor.getNames()[0] + " <" + availableSubcommands + ">";
        }
        return "This command is missing a handler.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String bindingError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandBindingException exception
    ) {
        return exception.getMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String executionError(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, RuntimeException exception) {
        return "An error occurred while executing the command.";
    }
}
