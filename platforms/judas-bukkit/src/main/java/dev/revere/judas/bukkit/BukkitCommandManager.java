package dev.revere.judas.bukkit;

import dev.revere.judas.bukkit.command.BukkitCommand;
import dev.revere.judas.bukkit.internal.BukkitCommandMapAccessor;
import dev.revere.judas.bukkit.resolver.BukkitParameterResolvers;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.runtime.CommandManager;
import dev.revere.judas.runtime.CommandManagerOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit implementation of {@link CommandManager}.
 */
public class BukkitCommandManager extends CommandManager {
    private final Plugin plugin;
    private final CommandMap commandMap;

    /**
     * @param plugin owning Bukkit plugin instance
     */
    public BukkitCommandManager(Plugin plugin) {
        this(plugin, CommandManagerOptions.builder().build());
    }

    /**
     * @param plugin owning Bukkit plugin instance
     * @param options runtime command framework options
     */
    public BukkitCommandManager(Plugin plugin, CommandManagerOptions options) {
        super(options);
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
        this.commandMap = BukkitCommandMapAccessor.resolve(Bukkit.getServer());
        this.registerResolver(Player.class, BukkitParameterResolvers.playerResolver());
        this.registerResolver(Player[].class, BukkitParameterResolvers.playerArrayResolver());
        this.registerResolver(OfflinePlayer.class, BukkitParameterResolvers.offlinePlayerResolver());
        this.registerResolver(World.class, BukkitParameterResolvers.worldResolver());
    }

    @Override
    protected void registerPlatform(CommandDescriptor descriptor) {
        BukkitCommand bukkitCommand = new BukkitCommand(descriptor, this);
        this.commandMap.register(this.plugin.getName().toLowerCase(), bukkitCommand);
    }
}
