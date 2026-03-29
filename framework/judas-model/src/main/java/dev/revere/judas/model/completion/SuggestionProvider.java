package dev.revere.judas.model.completion;

import java.util.List;

/**
 * Supplies tab-completion candidates for a parameter, for example dynamic ids from a service.
 */
@FunctionalInterface
public interface SuggestionProvider {

    /**
     * Produces completion candidates for the currently targeted parameter.
     *
     * @param context completion context for the active handler and parameter
     * @return candidate strings (may be filtered by prefix by the engine)
     */
    List<String> suggest(CompletionContext context);
}
