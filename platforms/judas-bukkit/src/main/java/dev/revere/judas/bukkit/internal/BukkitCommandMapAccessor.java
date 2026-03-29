package dev.revere.judas.bukkit.internal;

import org.bukkit.Server;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

/**
 * Resolves the server's {@link CommandMap} via reflection on {@link Server}.
 * Works on standard CraftBukkit, Spigot, and Paper (Paper remains Bukkit-compatible at this API level).
 */
public final class BukkitCommandMapAccessor {

    private BukkitCommandMapAccessor() {
    }

    public static CommandMap resolve(Server server) {
        if (server == null) {
            throw new IllegalArgumentException("server must not be null");
        }

        try {
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            Object value = commandMapField.get(server);
            if (!(value instanceof CommandMap)) {
                throw new IllegalStateException("Unexpected command map type: " + (value == null ? "null" : value.getClass()));
            }
            return (CommandMap) value;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access the server CommandMap. Is this a compatible CraftBukkit derivative?", exception);
        }
    }
}
