package dev.revere.judas.example.bukkit.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-memory kit model used by the demo plugin.
 */
public final class Kit {
    private final String id;
    private final String displayName;
    private final List<String> inventoryPreview;

    public Kit(String id, String displayName, List<String> inventoryPreview) {
        this.id = id;
        this.displayName = displayName;
        this.inventoryPreview = Collections.unmodifiableList(new ArrayList<>(inventoryPreview));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getInventoryPreview() {
        return inventoryPreview;
    }
}
