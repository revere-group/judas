package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.model.completion.SuggestionProvider;

/**
 * Immutable suggestion metadata derived from one {@code @Suggestions} annotation.
 *
 * <p>A parameter may define both a provider type and inline literals. Resolution order is applied later
 * by completion services; this class only carries normalized metadata.
 */
public final class ParameterSuggestionMetadata {
    private final Class<? extends SuggestionProvider> providerType;
    private final String[] inlineSuggestions;

    /**
     * Creates immutable suggestion metadata for one parameter.
     *
     * @param providerType suggestion provider type, or {@code null}
     * @param inlineSuggestions normalized inline literal suggestions
     */
    public ParameterSuggestionMetadata(Class<? extends SuggestionProvider> providerType, String[] inlineSuggestions) {
        this.providerType = providerType;
        this.inlineSuggestions = inlineSuggestions;
    }

    /**
     * Returns the configured suggestion-provider type.
     *
     * @return suggestion provider type, or {@code null}
     */
    public Class<? extends SuggestionProvider> getProviderType() {
        return providerType;
    }

    /**
     * Returns normalized inline literal suggestions.
     *
     * @return inline literal suggestions
     */
    public String[] getInlineSuggestions() {
        return inlineSuggestions;
    }
}
