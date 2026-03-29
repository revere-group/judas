package dev.revere.judas.example.cli.command;

import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Example suggestion provider used by {@link CliAnnotationShowcaseCommand}.
 */
public final class CliShowcaseSuggestions implements SuggestionProvider {
    private static final List<String> CHANNELS = Arrays.asList("general", "support", "staff");

    @Override
    public List<String> suggest(CompletionContext context) {
        return CHANNELS;
    }
}
