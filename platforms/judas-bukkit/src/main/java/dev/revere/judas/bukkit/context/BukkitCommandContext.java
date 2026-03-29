package dev.revere.judas.bukkit.context;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Bukkit implementation of {@link CommandContext}.
 */
public class BukkitCommandContext implements CommandContext {
    private final CommandSender sender;
    private final String[] arguments;

    /**
     * @param sender Bukkit sender
     * @param arguments raw command arguments
     */
    public BukkitCommandContext(CommandSender sender, String[] arguments) {
        this.sender = sender;
        this.arguments = arguments;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    @Override
    public void sendMessage(CommandColor color, String message) {
        sender.sendMessage(this.mapColor(color) + message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public String getSenderName() {
        return sender.getName();
    }

    @Override
    public Object getSender() {
        return sender;
    }

    /**
     * @return wrapped Bukkit sender
     */
    public CommandSender getBukkitSender() {
        return sender;
    }

    private ChatColor mapColor(CommandColor color) {
        switch (color) {
            case PRIMARY:
                return ChatColor.AQUA;
            case SECONDARY:
                return ChatColor.GRAY;
            case SUCCESS:
                return ChatColor.GREEN;
            case WARNING:
                return ChatColor.YELLOW;
            case ERROR:
                return ChatColor.RED;
            case INFO:
                return ChatColor.WHITE;
            case RESET:
            default:
                return ChatColor.RESET;
        }
    }
}
