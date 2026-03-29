package dev.revere.judas.engine.command.metadata;

import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * Builds implicit and explicit aliases for valued {@code Switch} and boolean {@code Flag} parameters.
 */
public final class ParameterOptionNameResolver {
    private ParameterOptionNameResolver() {
    }

    /**
     * Builds the final switch/flag alias array for one parameter.
     *
     * @param parameterName logical parameter name
     * @param explicitNames aliases supplied by annotations
     * @return normalized switch/flag alias array
     */
    public static String[] resolve(String parameterName, String[] explicitNames) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        String implicit = "--" + parameterName.toLowerCase(Locale.ROOT).replace('_', '-');
        unique.add(implicit);
        for (String explicit : explicitNames) {
            unique.add(normalize(explicit));
        }
        return unique.toArray(new String[0]);
    }

    /**
     * Normalizes one raw alias into canonical short or long form.
     *
     * @param name raw alias token
     * @return normalized alias token
     */
    public static String normalize(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Switch/flag alias cannot be empty.");
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
