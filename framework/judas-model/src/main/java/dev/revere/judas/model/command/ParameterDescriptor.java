package dev.revere.judas.model.command;

import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.Arrays;

/**
 * Describes a command handler parameter and its binding/completion metadata.
 */
public class ParameterDescriptor {
    private final String name;
    private final Class<?> type;
    private final boolean optional;
    private final String defaultValue;
    private final boolean consumeRemaining;
    private final boolean senderInjection;
    private final Class<? extends SuggestionProvider> suggestionProviderType;
    private final boolean flag;
    private final boolean positionalAllowed;
    private final String[] optionNames;

    /**
     * @param name logical parameter name
     * @param type Java parameter type
     * @param optional whether omission is allowed
     * @param defaultValue fallback string token, or {@code null}
     * @param consumeRemaining whether remaining tokens are joined
     * @param senderInjection whether this is a {@code @Sender} parameter
     * @param suggestionProviderType custom suggestion provider, or {@code null}
     * @param flag whether this is a boolean flag
     * @param positionalAllowed whether positional consumption is allowed for options
     * @param optionNames resolved option/flag aliases
     */
    public ParameterDescriptor(
            String name,
            Class<?> type,
            boolean optional,
            String defaultValue,
            boolean consumeRemaining,
            boolean senderInjection,
            Class<? extends SuggestionProvider> suggestionProviderType,
            boolean flag,
            boolean positionalAllowed,
            String[] optionNames
    ) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.consumeRemaining = consumeRemaining;
        this.senderInjection = senderInjection;
        this.suggestionProviderType = suggestionProviderType;
        this.flag = flag;
        this.positionalAllowed = positionalAllowed;
        this.optionNames = optionNames == null ? new String[0] : Arrays.copyOf(optionNames, optionNames.length);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isConsumeRemaining() {
        return consumeRemaining;
    }

    public boolean isSenderInjection() {
        return senderInjection;
    }

    /**
     * @return custom tab-completion provider type, or {@code null}
     */
    public Class<? extends SuggestionProvider> getSuggestionProviderType() {
        return suggestionProviderType;
    }

    /**
     * @return {@code true} when this parameter is a boolean flag/switch
     */
    public boolean isFlag() {
        return flag;
    }

    /**
     * @return {@code true} when this parameter may still be consumed positionally
     */
    public boolean isPositionalAllowed() {
        return positionalAllowed;
    }

    /**
     * @return option aliases accepted for this parameter
     */
    public String[] getOptionNames() {
        return Arrays.copyOf(optionNames, optionNames.length);
    }
}
