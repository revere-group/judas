package dev.revere.judas.example.bukkit.service;

import dev.revere.judas.example.bukkit.model.Arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory arena service for demo command functionality.
 */
public final class ArenaService {
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaService() {
        this.save(new Arena("duel", "ranked", true));
        this.save(new Arena("skywars", "casual", true));
        this.save(new Arena("lobby", "social", false));
    }

    public void save(Arena arena) {
        this.arenas.put(normalize(arena.getId()), arena);
    }

    public Arena findById(String id) {
        if (id == null) {
            return null;
        }
        return this.arenas.get(normalize(id));
    }

    public Collection<Arena> findAll() {
        return Collections.unmodifiableCollection(this.arenas.values());
    }

    public List<String> suggestIds(String partial) {
        String p = partial == null ? "" : normalize(partial);
        List<String> out = new ArrayList<>();
        for (Arena arena : this.arenas.values()) {
            if (p.isEmpty() || normalize(arena.getId()).startsWith(p)) {
                out.add(arena.getId());
            }
        }
        return out;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
