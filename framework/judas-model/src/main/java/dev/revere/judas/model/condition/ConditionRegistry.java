package dev.revere.judas.model.condition;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for named command condition implementations.
 */
public final class ConditionRegistry {
    private final Map<String, CommandCondition> conditions = new ConcurrentHashMap<>();

    /**
     * Registers a named condition implementation.
     *
     * <p>Condition keys are normalized case-insensitively so lookups are stable across annotation casing.
     *
     * @param key condition key used in {@code @Conditions}
     * @param condition condition implementation
     */
    public void register(String key, CommandCondition condition) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition key must not be empty.");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        this.conditions.put(normalize(key), condition);
    }

    /**
     * Resolves a condition by key.
     *
     * @param key condition key
     * @return registered condition or {@code null}
     */
    public CommandCondition get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return this.conditions.get(normalize(key));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
