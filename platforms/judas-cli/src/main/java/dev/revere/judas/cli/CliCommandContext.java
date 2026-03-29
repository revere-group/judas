package dev.revere.judas.cli;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;

/**
 * Command context implementation for CLI execution.
 */
public final class CliCommandContext implements CommandContext {
    private final CliCommandSender sender;
    private final String[] arguments;

    /**
     * Creates a CLI-backed command context.
     *
     * @param sender sender
     * @param arguments arguments after root token
     */
    public CliCommandContext(CliCommandSender sender, String[] arguments) {
        this.sender = sender;
        this.arguments = arguments;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

    @Override
    public void sendMessage(String message) {
        this.sender.sendMessage(message);
    }

    @Override
    public void sendMessage(CommandColor color, String message) {
        // CLI keeps the semantic color for parity with the API, but the default sender prints plain text.
        this.sender.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.sender.hasPermission(permission);
    }

    @Override
    public String getSenderName() {
        return this.sender.getName();
    }

    @Override
    public Object getSender() {
        return this.sender;
    }
}
