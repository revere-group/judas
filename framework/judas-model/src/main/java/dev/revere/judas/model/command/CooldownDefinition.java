package dev.revere.judas.model.command;

import dev.revere.judas.api.annotation.CooldownScope;

/**
 * Immutable cooldown metadata attached to a command handler.
 */
public final class CooldownDefinition {
    private final long durationMillis;
    private final CooldownScope scope;
    private final String key;

    /**
     * @param durationMillis cooldown duration in milliseconds
     * @param scope cooldown scope strategy
     * @param key optional custom key suffix
     */
    public CooldownDefinition(long durationMillis, CooldownScope scope, String key) {
        this.durationMillis = durationMillis;
        this.scope = scope;
        this.key = key;
    }

    /**
     * @return cooldown duration in milliseconds
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * @return cooldown scope strategy
     */
    public CooldownScope getScope() {
        return scope;
    }

    /**
     * @return optional custom key suffix, or empty string
     */
    public String getKey() {
        return key;
    }
}
