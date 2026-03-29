package dev.revere.judas.bukkit.resolver;

import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolveContext;
import dev.revere.judas.model.resolver.ParameterResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Bukkit-specific {@link ParameterResolver} registrations (online players, worlds, etc.).
 */
public final class BukkitParameterResolvers {

    private BukkitParameterResolvers() {
    }

    public static ParameterResolver<Player> playerResolver() {
        return new PlayerResolver();
    }

    public static ParameterResolver<Player[]> playerArrayResolver() {
        return new PlayerArrayResolver();
    }

    public static ParameterResolver<OfflinePlayer> offlinePlayerResolver() {
        return new OfflinePlayerResolver();
    }

    public static ParameterResolver<World> worldResolver() {
        return new WorldResolver();
    }

    private static final class PlayerResolver implements ParameterResolver<Player> {
        @Override
        public Player resolve(ParameterResolveContext context) {
            String input = ArgumentTokenReader.requireNext(context);

            Player player = Bukkit.getPlayer(input);
            if (player == null) {
                try {
                    player = Bukkit.getPlayer(UUID.fromString(input));
                } catch (IllegalArgumentException ignored) {
                    // not a UUID
                }
            }
            if (player == null) {
                throw new IllegalArgumentException("Player not found: " + input);
            }
            return player;
        }

        @Override
        public List<String> complete(CompletionContext context) {
            return completeOnlinePlayerNames(context);
        }
    }

    private static List<String> completeOnlinePlayerNames(CompletionContext context) {
        String partial = context.getPartial().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (partial.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                names.add(name);
            }
        }
        return names;
    }

    private static final class PlayerArrayResolver implements ParameterResolver<Player[]> {
        @Override
        public Player[] resolve(ParameterResolveContext context) {
            String input = ArgumentTokenReader.requireNext(context);
            if (input.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing player target(s).");
            }

            String normalized = input.trim();
            if ("*".equals(normalized) || "@a".equalsIgnoreCase(normalized)) {
                return snapshotOnlinePlayers();
            }

            String[] tokens = normalized.split("[\\s,]+");
            List<Player> players = new ArrayList<>();
            for (String token : tokens) {
                if (token.trim().isEmpty()) {
                    continue;
                }

                Player player = Bukkit.getPlayer(token.trim());
                if (player == null) {
                    throw new IllegalArgumentException("Player not found: " + token);
                }
                players.add(player);
            }

            return players.toArray(new Player[0]);
        }

        @Override
        public List<String> complete(CompletionContext context) {
            String partial = context.getPartial();
            String p = partial == null ? "" : partial;
            List<String> out = new ArrayList<>();
            if (p.isEmpty() || "*".startsWith(p)) {
                out.add("*");
            }
            if (p.isEmpty() || "@a".toLowerCase(Locale.ROOT).startsWith(p.toLowerCase(Locale.ROOT))) {
                out.add("@a");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                String name = online.getName();
                if (p.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(p.toLowerCase(Locale.ROOT))) {
                    out.add(name);
                }
            }
            return out;
        }
    }

    private static Player[] snapshotOnlinePlayers() {
        Object raw = Bukkit.getOnlinePlayers();
        if (raw == null) {
            return new Player[0];
        }
        if (raw instanceof Player[]) {
            Player[] array = (Player[]) raw;
            return Arrays.copyOf(array, array.length);
        }
        if (raw instanceof Collection) {
            List<Player> list = new ArrayList<>();
            for (Object element : (Collection<?>) raw) {
                if (element instanceof Player) {
                    list.add((Player) element);
                }
            }
            return list.toArray(new Player[0]);
        }
        throw new IllegalStateException("Unsupported getOnlinePlayers() return type: " + raw.getClass().getName());
    }

    private static final class OfflinePlayerResolver implements ParameterResolver<OfflinePlayer> {
        @Override
        public OfflinePlayer resolve(ParameterResolveContext context) {
            String input = ArgumentTokenReader.requireNext(context);

            OfflinePlayer player = tryResolveOfflinePlayer(input);
            if (player.isOnline() || player.hasPlayedBefore()) {
                return player;
            }

            throw new IllegalArgumentException("Player not found: " + input);
        }

        @Override
        public List<String> complete(CompletionContext context) {
            return completeOnlinePlayerNames(context);
        }

        @SuppressWarnings("deprecation")
        private static OfflinePlayer tryResolveOfflinePlayer(String input) {
            try {
                UUID uuid = UUID.fromString(input);
                return Bukkit.getOfflinePlayer(uuid);
            } catch (IllegalArgumentException ignored) {
                return Bukkit.getOfflinePlayer(input);
            }
        }
    }

    private static final class WorldResolver implements ParameterResolver<World> {
        @Override
        public World resolve(ParameterResolveContext context) {
            String input = ArgumentTokenReader.requireNext(context);
            World world = Bukkit.getWorld(input);
            if (world == null) {
                throw new IllegalArgumentException("World not found: " + input);
            }
            return world;
        }

        @Override
        public List<String> complete(CompletionContext context) {
            String partial = context.getPartial().toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                String name = world.getName();
                if (partial.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                    names.add(name);
                }
            }
            return names;
        }
    }
}
