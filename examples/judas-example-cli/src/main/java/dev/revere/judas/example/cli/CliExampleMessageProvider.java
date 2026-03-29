package dev.revere.judas.example.cli;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.spi.CommandMessageProvider;

/**
 * Demonstrates custom framework messages in the CLI example.
 */
public final class CliExampleMessageProvider implements CommandMessageProvider {
    @Override
    public String unknownRootCommand(String rootToken) {
        return "[error] Unknown command: " + rootToken;
    }

    @Override
    public String noPermissionForRoot(CommandDescriptor descriptor) {
        return "[error] You do not have permission for this command.";
    }

    @Override
    public String noPermissionForSubcommand(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor) {
        return "[error] You do not have permission for this subcommand.";
    }

    @Override
    public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
        String suffix = availableSubcommands == null || availableSubcommands.isEmpty()
                ? ""
                : " Available: " + availableSubcommands;
        return "[error] Unknown subcommand: " + token + "." + suffix;
    }

    @Override
    public String cooldownActive(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        return "[error] Cooldown active. Try again in " + seconds + "s.";
    }

    @Override
    public String conditionError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandConditionException exception
    ) {
        return "[error] " + exception.getMessage();
    }

    @Override
    public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
        if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            return "[usage] /" + descriptor.getNames()[0] + " <" + availableSubcommands + ">";
        }
        return "[error] This command has no executable handler.";
    }

    @Override
    public String bindingError(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, CommandBindingException exception) {
        return "[error] " + exception.getMessage();
    }

    @Override
    public String executionError(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, RuntimeException exception) {
        return "[error] Command execution failed.";
    }
}
