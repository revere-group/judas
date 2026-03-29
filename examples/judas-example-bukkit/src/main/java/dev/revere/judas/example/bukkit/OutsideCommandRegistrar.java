package dev.revere.judas.example.bukkit;

import dev.revere.judas.bukkit.BukkitCommandManager;
import dev.revere.judas.example.bukkit.feature.arena.ArenaExternalSubcommands;

/**
 * Demonstrates registration from a different module/location using plugin-held manager access.
 *
 * <p>This pattern is useful when commands are split by bounded context or feature package
 * and should not all be wired inside {@link ExamplePlugin}.
 */
public final class OutsideCommandRegistrar {

    private OutsideCommandRegistrar() {
    }

    public static void register(ExamplePlugin plugin) {
        BukkitCommandManager manager = plugin.getCommandManager();
        if (manager == null) {
            throw new IllegalStateException("Command manager is not initialized yet.");
        }

        // Auto-parent scenario:
        // "where" is declared with @Subcommand(parent = "arena"), so root is resolved automatically.
        manager.registerSubcommand(new ArenaExternalSubcommands(), "where");
    }
}
