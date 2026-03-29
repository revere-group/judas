package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Switch;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Suggestions;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Validates incompatible per-parameter annotation combinations before descriptor creation.
 *
 * <p>Rules apply to a single method parameter only. A handler may use {@link Switch} on one parameter and
 * {@link Flag} on another in the same method; combining both annotations on the same parameter is not allowed.
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
     * @param valueSwitch valued-switch annotation, or {@code null}
     * @param flag boolean flag annotation, or {@code null}
     */
    public static void validate(
            Method method,
            Parameter parameter,
            Sender sender,
            ConsumeRemaining consumeRemaining,
            Suggestions suggestions,
            Switch valueSwitch,
            Flag flag
    ) {
        if (sender != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Sender and @ConsumeRemaining.");
        }
        if (suggestions != null && sender != null) {
            throw invalid(method, parameter, "cannot combine @Sender and @Suggestions.");
        }
        if (flag != null && valueSwitch != null) {
            throw invalid(method, parameter, "cannot combine @Flag and @Switch on the same parameter.");
        }
        if (flag != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Flag and @ConsumeRemaining.");
        }
        if (valueSwitch != null && consumeRemaining != null) {
            throw invalid(method, parameter, "cannot combine @Switch and @ConsumeRemaining.");
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
