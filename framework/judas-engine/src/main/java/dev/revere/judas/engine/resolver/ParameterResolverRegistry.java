package dev.revere.judas.engine.resolver;

import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link ParameterResolver} instances keyed by type.
 */
public class ParameterResolverRegistry {
    private final Map<Class<?>, ParameterResolver<?>> resolvers = new HashMap<>();

    public <T> void register(Class<T> type, ParameterResolver<T> resolver) {
        this.resolvers.put(type, resolver);
    }

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
