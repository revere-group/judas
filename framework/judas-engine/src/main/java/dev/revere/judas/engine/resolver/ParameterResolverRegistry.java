package dev.revere.judas.engine.resolver;

import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link ParameterResolver} instances keyed by type.
 */
public final class ParameterResolverRegistry {
    private final Map<Class<?>, ParameterResolver<?>> resolvers = new HashMap<>();

    /**
     * Registers a typed parameter resolver.
     *
     * @param type resolved parameter type
     * @param resolver resolver implementation
     * @param <T> parameter type
     */
    public <T> void register(Class<T> type, ParameterResolver<T> resolver) {
        this.resolvers.put(type, resolver);
    }

    /**
     * Resolves the best-matching resolver for a parameter type.
     *
     * @param type parameter type to resolve
     * @param <T> parameter type
     * @return registered resolver, enum resolver, assignable resolver, or {@code null}
     */
    public <T> ParameterResolver<T> get(Class<T> type) {
        ParameterResolver<?> exact = this.resolvers.get(type);
        if (exact != null) {
            return cast(exact);
        }

        if (type.isEnum()) {
            return cast(EnumParameterResolverFactory.forEnum(type));
        }

        for (Map.Entry<Class<?>, ParameterResolver<?>> entry : this.resolvers.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return cast(entry.getValue());
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> ParameterResolver<T> cast(ParameterResolver<?> resolver) {
        return (ParameterResolver<T>) resolver;
    }
}
