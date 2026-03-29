package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.completion.SuggestionProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Builds {@link ParameterDescriptor} lists from reflective method metadata.
 */
public final class MethodParameterIntrospector {

    private MethodParameterIntrospector() {
    }

    /**
     * Describes all parameters declared on a command handler method.
     *
     * @param method command handler method
     * @return ordered parameter descriptors matching method signature order
     */
    public static List<ParameterDescriptor> describeParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterDescriptor> result = new ArrayList<>(parameters.length);

        for (Parameter parameter : parameters) {
            Name nameAnnotation = parameter.getAnnotation(Name.class);
            Optional optionalAnnotation = parameter.getAnnotation(Optional.class);
            Default defaultAnnotation = parameter.getAnnotation(Default.class);
            ConsumeRemaining consumeRemaining = parameter.getAnnotation(ConsumeRemaining.class);
            Sender sender = parameter.getAnnotation(Sender.class);
            Suggestions suggestions = parameter.getAnnotation(Suggestions.class);
            Option option = parameter.getAnnotation(Option.class);
            Flag flag = parameter.getAnnotation(Flag.class);

            if (sender != null && consumeRemaining != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Sender and @ConsumeRemaining."
                );
            }

            if (suggestions != null && sender != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Sender and @Suggestions."
                );
            }

            if (flag != null && option != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Flag and @Option."
                );
            }

            if (flag != null && consumeRemaining != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Flag and @ConsumeRemaining."
                );
            }

            if (option != null && consumeRemaining != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Option and @ConsumeRemaining."
                );
            }

            if (flag != null && suggestions != null) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " cannot combine @Flag and @Suggestions."
                );
            }

            boolean isFlag = flag != null;
            if (isFlag && !(parameter.getType() == Boolean.class || parameter.getType() == boolean.class)) {
                throw new IllegalArgumentException(
                        "Parameter '" + parameter.getName() + "' of " + method + " uses @Flag but is not boolean/Boolean."
                );
            }

            Class<? extends SuggestionProvider> suggestionType = null;
            if (suggestions != null) {
                Class<?> raw = suggestions.value();
                if (!SuggestionProvider.class.isAssignableFrom(raw)) {
                    throw new IllegalArgumentException(
                            "Parameter '" + parameter.getName() + "' of " + method
                                    + ": @Suggestions value must implement SuggestionProvider."
                    );
                }
                suggestionType = raw.asSubclass(SuggestionProvider.class);
            }

            boolean positionalAllowed = option == null || option.positional();
            String[] optionNames = new String[0];
            if (sender == null && !CommandContext.class.isAssignableFrom(parameter.getType())) {
                String[] explicitNames;
                if (option != null) {
                    explicitNames = option.names();
                } else if (flag != null) {
                    explicitNames = flag.names();
                } else {
                    explicitNames = new String[0];
                }
                optionNames = buildOptionNames(
                        nameAnnotation != null ? nameAnnotation.value() : parameter.getName(),
                        explicitNames
                );
            }

            result.add(new ParameterDescriptor(
                    nameAnnotation != null ? nameAnnotation.value() : parameter.getName(),
                    parameter.getType(),
                    optionalAnnotation != null,
                    defaultAnnotation != null ? defaultAnnotation.value() : null,
                    consumeRemaining != null,
                    sender != null,
                    suggestionType,
                    isFlag,
                    positionalAllowed,
                    optionNames
            ));
        }

        return result;
    }

    /**
     * Builds implicit + explicit option aliases for a parameter.
     *
     * @param parameterName logical parameter name
     * @param explicitNames aliases provided by annotation
     * @return normalized alias set as array (in insertion order)
     */
    private static String[] buildOptionNames(String parameterName, String[] explicitNames) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        String implicit = "--" + parameterName.toLowerCase(Locale.ROOT).replace('_', '-');
        unique.add(implicit);

        for (String explicit : explicitNames) {
            String normalized = normalizeOptionName(explicit);
            unique.add(normalized);
        }
        return unique.toArray(new String[0]);
    }

    /**
     * Normalizes an alias token to either short ({@code -x}) or long ({@code --name}) form.
     *
     * @param name raw alias token
     * @return normalized alias token
     */
    private static String normalizeOptionName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Option/flag alias cannot be empty.");
        }
        if (value.startsWith("--") || value.startsWith("-")) {
            return value;
        }
        if (value.length() == 1) {
            return "-" + value;
        }
        return "--" + value;
    }
}
