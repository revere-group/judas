package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves suggestion-provider and inline-literal metadata for one parameter.
 */
public final class ParameterSuggestionMetadataResolver {
    private ParameterSuggestionMetadataResolver() {
    }

    /**
     * Resolves normalized suggestion metadata for one reflective parameter.
     *
     * @param method declaring command method
     * @param parameter reflective parameter
     * @param suggestions suggestions annotation, or {@code null}
     * @return resolved suggestion metadata
     */
    public static ParameterSuggestionMetadata resolve(Method method, Parameter parameter, Suggestions suggestions) {
        Class<? extends SuggestionProvider> suggestionType = null;
        String[] inlineSuggestions = new String[0];
        if (suggestions != null) {
            Class<?> raw = suggestions.value();
            if (raw != Void.class) {
                if (!SuggestionProvider.class.isAssignableFrom(raw)) {
                    throw new IllegalArgumentException(
                            "Parameter '" + parameter.getName() + "' of " + method
                                    + ": @Suggestions value must implement SuggestionProvider."
                    );
                }
                suggestionType = raw.asSubclass(SuggestionProvider.class);
            }
            inlineSuggestions = normalizeInlineSuggestions(suggestions.literals());
            if (suggestionType == null && inlineSuggestions.length == 0) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method
                                + ": @Suggestions must provide a provider class or literals."
                );
            }
        }
        return new ParameterSuggestionMetadata(suggestionType, inlineSuggestions);
    }

    private static String[] normalizeInlineSuggestions(String[] values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            out.add(value.trim());
        }
        return out.toArray(new String[0]);
    }
}
