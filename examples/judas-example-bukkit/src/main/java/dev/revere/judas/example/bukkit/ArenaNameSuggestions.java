package dev.revere.judas.example.bukkit;

import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Scenario: parameter completion provider.
 *
 * <p>Bound via {@code @Suggestions(ArenaNameSuggestions.class)} on a parameter.
 * In production, replace the static list with repository/service calls.
 */
public final class ArenaNameSuggestions implements SuggestionProvider {

    private static final List<String> EXAMPLE_ARENAS = Arrays.asList("duel", "skywars", "lobby");

    @Override
    public List<String> suggest(CompletionContext context) {
        return EXAMPLE_ARENAS;
    }
}
