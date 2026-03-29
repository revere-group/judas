package dev.revere.judas.engine.command;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses command tokens into positional arguments, named options and flags.
 */
public final class CommandInputParser {

    private CommandInputParser() {
    }

    /**
     * Parses user input for execution/binding flow.
     *
     * @param tokens raw argument tokens
     * @param parameters handler parameter descriptors
     * @return parsed input model for binding
     */
    public static ParsedCommandInput parseForBinding(String[] tokens, List<ParameterDescriptor> parameters) {
        return parse(tokens, parameters, true, false);
    }

    /**
     * Parses user input for completion flow.
     *
     * @param tokens raw argument tokens
     * @param parameters handler parameter descriptors
     * @return parsed input model for completion
     */
    public static ParsedCommandInput parseForCompletion(String[] tokens, List<ParameterDescriptor> parameters) {
        return parse(tokens, parameters, false, true);
    }

    private static ParsedCommandInput parse(
            String[] tokens,
            List<ParameterDescriptor> parameters,
            boolean strictUnknownOption,
            boolean allowPendingValue
    ) {
        Map<String, ParameterDescriptor> valueAliases = new LinkedHashMap<>();
        Map<String, ParameterDescriptor> flagAliases = new LinkedHashMap<>();
        for (ParameterDescriptor parameter : parameters) {
            String[] aliases = parameter.getOptionNames();
            if (aliases == null) {
                continue;
            }
            for (String alias : aliases) {
                if (parameter.isFlag()) {
                    flagAliases.put(alias, parameter);
                } else {
                    valueAliases.put(alias, parameter);
                }
            }
        }

        List<String> positional = new ArrayList<>();
        Map<ParameterDescriptor, String> namedValues = new LinkedHashMap<>();
        Set<ParameterDescriptor> flags = new LinkedHashSet<>();

        ParameterDescriptor waitingValueFor = null;
        ParameterDescriptor pendingValueParameter = null;
        String pendingValuePrefix = "";
        String optionPrefix = null;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            boolean lastToken = i == tokens.length - 1;

            if (waitingValueFor != null) {
                if (allowPendingValue && lastToken) {
                    pendingValueParameter = waitingValueFor;
                    pendingValuePrefix = token;
                    waitingValueFor = null;
                    continue;
                }
                namedValues.put(waitingValueFor, token);
                waitingValueFor = null;
                continue;
            }

            String optionKey = token;
            String inlineValue = null;
            int equalsAt = token.indexOf('=');
            if (equalsAt > 0) {
                optionKey = token.substring(0, equalsAt);
                inlineValue = token.substring(equalsAt + 1);
            }

            ParameterDescriptor flagParameter = flagAliases.get(optionKey);
            if (flagParameter != null) {
                flags.add(flagParameter);
                continue;
            }

            ParameterDescriptor valueParameter = valueAliases.get(optionKey);
            if (valueParameter != null) {
                if (inlineValue != null) {
                    if (allowPendingValue && lastToken) {
                        pendingValueParameter = valueParameter;
                        pendingValuePrefix = inlineValue;
                    } else {
                        namedValues.put(valueParameter, inlineValue);
                    }
                    continue;
                }

                if (lastToken) {
                    if (allowPendingValue) {
                        pendingValueParameter = valueParameter;
                        pendingValuePrefix = "";
                        continue;
                    }
                    throw new CommandBindingException("Missing value for option: " + optionKey);
                }

                waitingValueFor = valueParameter;
                continue;
            }

            if (token.startsWith("-") && !isLikelyNumeric(token)) {
                if (allowPendingValue && lastToken) {
                    optionPrefix = token;
                    continue;
                }

                if (strictUnknownOption) {
                    throw new CommandBindingException("Unknown option: " + token);
                }
            }

            positional.add(token);
        }

        if (waitingValueFor != null) {
            if (allowPendingValue) {
                pendingValueParameter = waitingValueFor;
                pendingValuePrefix = "";
            } else {
                throw new CommandBindingException("Missing value for option: " + firstAlias(waitingValueFor));
            }
        }

        return new ParsedCommandInput(
                positional,
                namedValues,
                flags,
                pendingValueParameter,
                pendingValuePrefix,
                optionPrefix
        );
    }

    private static String firstAlias(ParameterDescriptor parameter) {
        String[] names = parameter.getOptionNames();
        if (names == null || names.length == 0) {
            return parameter.getName();
        }
        return names[0];
    }

    private static boolean isLikelyNumeric(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '-') {
            return false;
        }
        int start = 1;
        boolean hasDigit = false;
        for (int i = start; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch >= '0' && ch <= '9') {
                hasDigit = true;
                continue;
            }
            if (ch == '.') {
                continue;
            }
            return false;
        }
        return hasDigit;
    }
}
