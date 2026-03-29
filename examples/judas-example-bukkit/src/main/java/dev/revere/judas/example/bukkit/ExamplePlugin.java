package dev.revere.judas.example.bukkit;

import dev.revere.judas.bukkit.BukkitCommandManager;
import dev.revere.judas.example.bukkit.feature.arena.ArenaSubcommands;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example plugin entry point that demonstrates every supported manual registration scenario.
 *
 * <p>Scenarios shown here:
 * <ul>
 *     <li>Registering full root-command holders with {@code register(...)}.</li>
 *     <li>Registering all subcommands from another holder under an explicit root.</li>
 *     <li>Registering selected subcommands under an explicit root.</li>
 *     <li>Auto-resolving root from {@code @Subcommand(parent = "...")}.</li>
 *     <li>Registering commands from outside this class via manager access.</li>
 * </ul>
 */
public class ExamplePlugin extends JavaPlugin {
    private BukkitCommandManager commandManager;

    @Override
    public void onEnable() {
        // Core framework entry point for Bukkit.
        this.commandManager = new BukkitCommandManager(this);

        // Scenario 1: Regular holder registration.
        // Each holder can define one or multiple root commands through @Definition.
        this.commandManager.register(new DisguiseCommand());
        this.commandManager.register(new PingCommand());
        this.commandManager.register(new ArenaCommand());
        this.commandManager.register(new SupportCommandHolder());

        // Scenario 2: Register all subcommands from ArenaSubcommands under "arena".
        // Parentless + parent="arena" entries are included.
        // Entries explicitly targeting a different parent (e.g. parent="kit") are skipped.
        this.commandManager.registerSubcommands("arena", new ArenaSubcommands());

        // Scenario 3: Register only specific aliases under an explicit root.
        // This throws if an alias does not exist on the holder or targets a different explicit parent.
        // this.commandManager.registerSubcommands("arena", new ArenaSubcommands(), "create", "view");

        // Scenario 4: Auto-resolve target root from @Subcommand(parent = "...").
        // This is useful when holders are organized by feature package instead of root command.
        // this.commandManager.registerSubcommand(new ArenaExternalSubcommands(), "where");
        // this.commandManager.registerSubcommand(new ArenaExternalSubcommands());

        // Scenario 5: Registration in a different class/module.
        // The plugin exposes getCommandManager(), so other bootstrap code can wire commands too.
        OutsideCommandRegistrar.register(this);
    }

    public BukkitCommandManager getCommandManager() {
        return this.commandManager;
    }
}
