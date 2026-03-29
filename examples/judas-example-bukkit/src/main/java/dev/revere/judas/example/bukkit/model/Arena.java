package dev.revere.judas.example.bukkit.model;

/**
 * Simple in-memory arena model used by the demo plugin.
 */
public final class Arena {
    private final String id;
    private final String mode;
    private boolean enabled;

    public Arena(String id, String mode, boolean enabled) {
        this.id = id;
        this.mode = mode;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getMode() {
        return mode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
