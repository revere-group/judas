package dev.revere.judas.example.bukkit.service;

import dev.revere.judas.example.bukkit.model.Kit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory kit service for demo command functionality.
 */
public final class KitService {
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitService() {
        this.save(new Kit("archer", "Archer", Arrays.asList("bow", "arrow", "leather_armor")));
        this.save(new Kit("tank", "Tank", Arrays.asList("iron_chestplate", "shield", "golden_apple")));
        this.save(new Kit("assassin", "Assassin", Arrays.asList("stone_sword", "ender_pearl", "speed_potion")));
    }

    public void save(Kit kit) {
        this.kits.put(normalize(kit.getId()), kit);
    }

    public Kit findById(String id) {
        if (id == null) {
            return null;
        }
        return this.kits.get(normalize(id));
    }

    public Collection<Kit> findAll() {
        return Collections.unmodifiableCollection(this.kits.values());
    }

    public List<String> suggestIds(String partial) {
        String p = partial == null ? "" : normalize(partial);
        List<String> out = new ArrayList<>();
        for (Kit kit : this.kits.values()) {
            if (p.isEmpty() || normalize(kit.getId()).startsWith(p)) {
                out.add(kit.getId());
            }
        }
        return out;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
