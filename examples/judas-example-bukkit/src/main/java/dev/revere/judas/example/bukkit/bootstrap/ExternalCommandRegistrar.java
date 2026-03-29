package dev.revere.judas.example.bukkit.bootstrap;

import dev.revere.judas.bukkit.BukkitCommandManager;
import dev.revere.judas.example.bukkit.ExamplePlugin;
import dev.revere.judas.example.bukkit.command.arena.ArenaExternalSubcommands;
import dev.revere.judas.example.bukkit.command.support.SupportCommandHolder;

/**
 * Demonstrates command registration outside plugin main class.
 */
public final class ExternalCommandRegistrar {
    private ExternalCommandRegistrar() {
    }

    /**
     * Registers a couple of extra commands from outside the plugin bootstrap class.
     *
     * @param plugin example plugin exposing the initialized command manager
     */
    public static void register(ExamplePlugin plugin) {
        BukkitCommandManager manager = plugin.getCommandManager();
        if (manager == null) {
            throw new IllegalStateException("Command manager is not initialized.");
        }

        // Auto-parent registration using @Subcommand(parent = "...").
        // The explicit alias here selects only the "where" subcommand from the holder.
        manager.registerSub(new ArenaExternalSubcommands(), "where");

        // Register roots from a different bootstrap location.
        manager.register(new SupportCommandHolder());
    }
}
