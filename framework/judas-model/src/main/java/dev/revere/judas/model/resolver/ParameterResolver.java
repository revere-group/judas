package dev.revere.judas.model.resolver;

import dev.revere.judas.model.completion.CompletionContext;

import java.util.Collections;
import java.util.List;

/**
 * Resolves a command parameter from a list of arguments.
 *
 * @param <T> the type of the parameter
 */
@FunctionalInterface
public interface ParameterResolver<T> {

    T resolve(ParameterResolveContext context);

    /**
     * Tab-completion candidates for this parameter type. Override for enums, players, worlds, etc.
     *
     * @param context completion context for the active parameter
     * @return candidate strings
     */
    default List<String> complete(CompletionContext context) {
        return Collections.emptyList();
    }
}
