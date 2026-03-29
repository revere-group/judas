package dev.revere.judas.engine.command.input;

import dev.revere.judas.model.command.ParameterDescriptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable lookup index for option and flag aliases.
 *
 * <p>This index is built once per parse pass and enables O(1) alias resolution while processing input tokens.
 * Aliases for value options and boolean flags are kept in separate maps so parser behavior is explicit and
 * unambiguous.
 */
public final class CommandInputAliasIndex {
    private final Map<String, ParameterDescriptor> valueAliases;
    private final Map<String, ParameterDescriptor> flagAliases;

    private CommandInputAliasIndex(
            Map<String, ParameterDescriptor> valueAliases,
            Map<String, ParameterDescriptor> flagAliases
    ) {
        this.valueAliases = valueAliases;
        this.flagAliases = flagAliases;
    }

    /**
     * Creates an alias index for one handler parameter list.
     *
     * <p>When duplicate aliases exist, the latest declaration in iteration order wins.
     * This method does not perform duplicate-alias validation; validation belongs to metadata creation.
     *
     * @param parameters handler parameter descriptors used to collect option and flag aliases
     * @return populated alias index used by input parsing
     */
    public static CommandInputAliasIndex create(List<ParameterDescriptor> parameters) {
        Map<String, ParameterDescriptor> valueAliases = new LinkedHashMap<>();
        Map<String, ParameterDescriptor> flagAliases = new LinkedHashMap<>();
        for (ParameterDescriptor parameter : parameters) {
            for (String alias : parameter.getOptionNames()) {
                if (parameter.isFlag()) {
                    flagAliases.put(alias, parameter);
                } else {
                    valueAliases.put(alias, parameter);
                }
            }
        }
        return new CommandInputAliasIndex(valueAliases, flagAliases);
    }

    /**
     * Resolves a value-option alias.
     *
     * @param alias raw alias token such as {@code --times} or {@code -t}
     * @return matching value-option parameter descriptor, or {@code null} when unknown
     */
    public ParameterDescriptor findValueParameter(String alias) {
        return this.valueAliases.get(alias);
    }

    /**
     * Resolves a flag alias.
     *
     * @param alias raw alias token such as {@code --silent} or {@code -s}
     * @return matching flag parameter descriptor, or {@code null} when unknown
     */
    public ParameterDescriptor findFlagParameter(String alias) {
        return this.flagAliases.get(alias);
    }
}
