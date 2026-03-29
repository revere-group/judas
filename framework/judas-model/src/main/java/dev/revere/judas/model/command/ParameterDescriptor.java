package dev.revere.judas.model.command;

import dev.revere.judas.model.completion.SuggestionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private final String[] inlineSuggestions;
    private final boolean flag;
    private final boolean positionalAllowed;
    private final String[] optionNames;
    private final List<String> conditions;

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
        this(
                name,
                type,
                optional,
                defaultValue,
                consumeRemaining,
                senderInjection,
                suggestionProviderType,
                new String[0],
                flag,
                positionalAllowed,
                optionNames,
                Collections.<String>emptyList()
        );
    }

    /**
     * @param name logical parameter name
     * @param type Java parameter type
     * @param optional whether omission is allowed
     * @param defaultValue fallback string token, or {@code null}
     * @param consumeRemaining whether remaining tokens are joined
     * @param senderInjection whether this is a {@code @Sender} parameter
     * @param suggestionProviderType custom suggestion provider, or {@code null}
     * @param inlineSuggestions static inline suggestions for this parameter
     * @param flag whether this is a boolean flag
     * @param positionalAllowed whether positional consumption is allowed for options
     * @param optionNames resolved option/flag aliases
     * @param conditions parameter-level condition expressions
     */
    public ParameterDescriptor(
            String name,
            Class<?> type,
            boolean optional,
            String defaultValue,
            boolean consumeRemaining,
            boolean senderInjection,
            Class<? extends SuggestionProvider> suggestionProviderType,
            String[] inlineSuggestions,
            boolean flag,
            boolean positionalAllowed,
            String[] optionNames,
            List<String> conditions
    ) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.consumeRemaining = consumeRemaining;
        this.senderInjection = senderInjection;
        this.suggestionProviderType = suggestionProviderType;
        this.inlineSuggestions = inlineSuggestions == null ? new String[0] : Arrays.copyOf(inlineSuggestions, inlineSuggestions.length);
        this.flag = flag;
        this.positionalAllowed = positionalAllowed;
        this.optionNames = optionNames == null ? new String[0] : Arrays.copyOf(optionNames, optionNames.length);
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
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
     * @return inline static suggestions declared for this parameter
     */
    public String[] getInlineSuggestions() {
        return Arrays.copyOf(inlineSuggestions, inlineSuggestions.length);
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

    /**
     * @return immutable parameter-level condition expression list
     */
    public List<String> getConditions() {
        return conditions;
    }
}
