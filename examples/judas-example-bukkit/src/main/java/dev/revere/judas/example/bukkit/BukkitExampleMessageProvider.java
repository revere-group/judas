package dev.revere.judas.example.bukkit;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.spi.CommandMessageProvider;
import org.bukkit.ChatColor;

/**
 * Demonstrates Bukkit-colored framework messages.
 */
public final class BukkitExampleMessageProvider implements CommandMessageProvider {
    @Override
    public String unknownRootCommand(String rootToken) {
        return ChatColor.RED + "Unknown command " + ChatColor.WHITE + rootToken + ChatColor.RED + ".";
    }

    @Override
    public String noPermissionForRoot(CommandDescriptor descriptor) {
        return ChatColor.RED + "You do not have permission to use this command.";
    }

    @Override
    public String noPermissionForSubcommand(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor) {
        return ChatColor.RED + "You do not have permission to use this subcommand.";
    }

    @Override
    public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
        String suffix = availableSubcommands == null || availableSubcommands.isEmpty()
                ? ""
                : " " + ChatColor.YELLOW + "Available: " + availableSubcommands;
        return ChatColor.RED + "Unknown subcommand " + ChatColor.WHITE + token + ChatColor.RED + "." + suffix;
    }

    @Override
    public String cooldownActive(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        return ChatColor.RED + "You can use this again in " + ChatColor.YELLOW + seconds + "s" + ChatColor.RED + ".";
    }

    @Override
    public String conditionError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandConditionException exception
    ) {
        return ChatColor.RED + exception.getMessage();
    }

    @Override
    public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
        if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            return ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/" + descriptor.getNames()[0] + " <" + availableSubcommands + ">";
        }
        return ChatColor.RED + "This command has no executable handler.";
    }

    @Override
    public String bindingError(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, CommandBindingException exception) {
        return ChatColor.RED + exception.getMessage();
    }

    @Override
    public String executionError(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, RuntimeException exception) {
        return ChatColor.RED + "An internal error occurred while executing this command.";
    }
}
