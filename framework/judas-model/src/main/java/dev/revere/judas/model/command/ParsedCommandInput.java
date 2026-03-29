package dev.revere.judas.model.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parsed representation of command input with positional args, named options, and flags.
 */
public final class ParsedCommandInput {
    private final List<String> positionalTokens;
    private final Map<ParameterDescriptor, String> namedValues;
    private final Set<ParameterDescriptor> presentFlags;
    private final ParameterDescriptor pendingValueParameter;
    private final String pendingValuePrefix;
    private final String optionPrefix;

    /**
     * @param positionalTokens positional tokens in order
     * @param namedValues mapped option values
     * @param presentFlags flags that were present
     * @param pendingValueParameter option awaiting a value during completion
     * @param pendingValuePrefix partial value prefix when completing an option value
     * @param optionPrefix partial option token when completing an option name
     */
    public ParsedCommandInput(
            List<String> positionalTokens,
            Map<ParameterDescriptor, String> namedValues,
            Set<ParameterDescriptor> presentFlags,
            ParameterDescriptor pendingValueParameter,
            String pendingValuePrefix,
            String optionPrefix
    ) {
        this.positionalTokens = positionalTokens;
        this.namedValues = namedValues;
        this.presentFlags = presentFlags;
        this.pendingValueParameter = pendingValueParameter;
        this.pendingValuePrefix = pendingValuePrefix;
        this.optionPrefix = optionPrefix;
    }

    public List<String> getPositionalTokens() {
        return Collections.unmodifiableList(this.positionalTokens);
    }

    public Map<ParameterDescriptor, String> getNamedValues() {
        return Collections.unmodifiableMap(this.namedValues);
    }

    public Set<ParameterDescriptor> getPresentFlags() {
        return Collections.unmodifiableSet(this.presentFlags);
    }

    public boolean hasNamedValue(ParameterDescriptor parameter) {
        return this.namedValues.containsKey(parameter);
    }

    public String getNamedValue(ParameterDescriptor parameter) {
        return this.namedValues.get(parameter);
    }

    public boolean hasFlag(ParameterDescriptor parameter) {
        return this.presentFlags.contains(parameter);
    }

    public ParameterDescriptor getPendingValueParameter() {
        return this.pendingValueParameter;
    }

    public String getPendingValuePrefix() {
        return this.pendingValuePrefix;
    }

    public String getOptionPrefix() {
        return this.optionPrefix;
    }
}
