package dev.revere.judas.bukkit.logging;

import dev.revere.judas.model.spi.JudasLogger;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Bukkit-backed logger implementation for Judas internal framework logs.
 */
public final class BukkitJudasLogger implements JudasLogger {
    private final Plugin plugin;

    /**
     * @param plugin owning plugin whose logger should receive Judas framework messages
     */
    public BukkitJudasLogger(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String message) {
        this.plugin.getLogger().info("[Judas] " + message);
    }

    @Override
    public void warn(String message) {
        this.plugin.getLogger().warning("[Judas] " + message);
    }

    @Override
    public void debug(String message) {
        this.plugin.getLogger().fine("[Judas] " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        this.plugin.getLogger().log(Level.SEVERE, "[Judas] " + message, throwable);
    }
}
