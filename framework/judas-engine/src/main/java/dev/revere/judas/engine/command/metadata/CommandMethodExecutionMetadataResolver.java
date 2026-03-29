package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Async;
import dev.revere.judas.api.annotation.Cooldown;
import dev.revere.judas.model.command.CooldownDefinition;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Resolves effective execution metadata for command methods from class and method annotations.
 */
public final class CommandMethodExecutionMetadataResolver {
    private CommandMethodExecutionMetadataResolver() {
    }

    /**
     * Resolves whether asynchronous execution applies to a command method.
     *
     * @param ownerType declaring command holder type
     * @param method command method
     * @return {@code true} when async execution applies
     */
    public static boolean resolveAsyncExecution(Class<?> ownerType, Method method) {
        return method.isAnnotationPresent(Async.class) || ownerType.isAnnotationPresent(Async.class);
    }

    /**
     * Resolves the effective cooldown metadata for a method/class pair.
     *
     * @param ownerType declaring command holder type
     * @param method command method
     * @return effective cooldown metadata, or {@code null}
     */
    public static CooldownDefinition resolveCooldown(Class<?> ownerType, Method method) {
        Cooldown methodCooldown = method.getAnnotation(Cooldown.class);
        Cooldown typeCooldown = ownerType.getAnnotation(Cooldown.class);
        Cooldown resolved = methodCooldown != null ? methodCooldown : typeCooldown;
        if (resolved == null) {
            return null;
        }
        long durationMillis = resolved.unit().toMillis(resolved.value());
        if (durationMillis <= 0L) {
            return null;
        }
        return new CooldownDefinition(durationMillis, resolved.scope(), normalizeKey(resolved.key()));
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
