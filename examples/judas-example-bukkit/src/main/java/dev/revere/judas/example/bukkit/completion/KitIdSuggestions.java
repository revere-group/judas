package dev.revere.judas.example.bukkit.completion;

import dev.revere.judas.example.bukkit.service.KitService;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.List;

/**
 * Suggests kit ids from in-memory service.
 *
 * <p>Used for String parameters only. For {@code Kit} parameters, {@code KitParameterResolver.complete(...)}
 * already provides completion.
 */
public final class KitIdSuggestions implements SuggestionProvider {
    private final KitService kitService;

    public KitIdSuggestions(KitService kitService) {
        this.kitService = kitService;
    }

    @Override
    public List<String> suggest(CompletionContext context) {
        return this.kitService.suggestIds(context.getPartial());
    }
}
