package dev.revere.judas.cli;

import dev.revere.judas.cli.logging.CliJudasLogger;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.runtime.CommandManager;
import dev.revere.judas.runtime.CommandManagerOptions;

import java.util.Arrays;
import java.util.Locale;

/**
 * CLI runtime implementation proving Judas can run outside Minecraft.
 */
public class CliCommandManager extends CommandManager {

    public CliCommandManager() {
        super(CommandManagerOptions.builder().logger(new CliJudasLogger()).build());
    }

    /**
     * Creates a CLI manager with explicit runtime options.
     *
     * @param options runtime customization options
     */
    public CliCommandManager(CommandManagerOptions options) {
        super(options);
    }

    @Override
    protected void registerPlatform(CommandDescriptor descriptor) {
        // no platform command map required for CLI; descriptors are already tracked by CommandManager
    }

    /**
     * Executes one raw input line.
     *
     * @param sender sender implementation
     * @param input raw input line
     * @return {@code true} when a command root matched and dispatch ran
     */
    public boolean execute(CliCommandSender sender, String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String normalized = input.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        String[] split = normalized.split("\\s+");
        if (split.length == 0) {
            return false;
        }

        String rootName = split[0].toLowerCase(Locale.ROOT);
        CommandDescriptor descriptor = this.getCommands().get(rootName);
        if (descriptor == null) {
            sender.sendMessage(this.getMessageProvider().unknownRootCommand(split[0]));
            return false;
        }

        String[] args = split.length > 1 ? Arrays.copyOfRange(split, 1, split.length) : new String[0];
        this.dispatch(descriptor, new CliCommandContext(sender, args));
        return true;
    }
}
