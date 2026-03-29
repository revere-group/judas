package dev.revere.judas.example.bukkit.completion;

import dev.revere.judas.example.bukkit.service.ArenaService;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.List;

/**
 * Suggests arena ids from in-memory service.
 */
public final class ArenaIdSuggestions implements SuggestionProvider {
    private final ArenaService arenaService;

    public ArenaIdSuggestions(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    @Override
    public List<String> suggest(CompletionContext context) {
        return this.arenaService.suggestIds(context.getPartial());
    }
}
