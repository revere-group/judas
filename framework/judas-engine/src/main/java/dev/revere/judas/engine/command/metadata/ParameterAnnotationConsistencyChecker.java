package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Suggestions;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Validates incompatible parameter annotation combinations before descriptor creation.
 */
public final class ParameterAnnotationConsistencyChecker {
    private ParameterAnnotationConsistencyChecker() {
    }

    /**
     * Validates one parameter annotation set and throws on incompatible combinations.
     *
     * @param method declaring command method
     * @param parameter reflective parameter
     * @param sender sender annotation, or {@code null}
     * @param consumeRemaining consume-remaining annotation, or {@code null}
     * @param suggestions suggestions annotation, or {@code null}
     * @param option option annotation, or {@code null}
     * @param flag flag annotation, or {@code null}
     */
    public static void validate(
            Method method,
            Parameter parameter,
            Sender sender,
            ConsumeRemaining consumeRemaining,
            Suggestions suggestions,
            Option option,
            Flag flag
    ) {
        if (sender != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Sender and @ConsumeRemaining.");
        }
        if (suggestions != null && sender != null) {
            throw invalid(method, parameter, "cannot combine @Sender and @Suggestions.");
        }
        if (flag != null && option != null) {
            throw invalid(method, parameter, "cannot combine @Flag and @Option.");
        }
        if (flag != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Flag and @ConsumeRemaining.");
        }
        if (option != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Option and @ConsumeRemaining.");
        }
        if (flag != null && suggestions != null) {
            throw invalid(method, parameter, "cannot combine @Flag and @Suggestions.");
        }
        if (flag != null && !(parameter.getType() == Boolean.class || parameter.getType() == boolean.class)) {
            throw invalid(method, parameter, "uses @Flag but is not boolean/Boolean.");
        }
    }

    private static IllegalArgumentException invalid(Method method, Parameter parameter, String reason) {
        return new IllegalArgumentException("Parameter '" + parameter.getName() + "' of " + method + " " + reason);
    }
}
