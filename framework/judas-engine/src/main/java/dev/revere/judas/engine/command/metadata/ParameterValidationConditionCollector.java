package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Length;
import dev.revere.judas.api.annotation.Max;
import dev.revere.judas.api.annotation.Min;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects all condition expressions implied by parameter validation annotations.
 */
public final class ParameterValidationConditionCollector {
    private ParameterValidationConditionCollector() {
    }

    /**
     * Collects all condition expressions implied by explicit and validation annotations.
     *
     * @param conditions explicit conditions annotation, or {@code null}
     * @param range range annotation, or {@code null}
     * @param min min annotation, or {@code null}
     * @param max max annotation, or {@code null}
     * @param length length annotation, or {@code null}
     * @param regex regex annotation, or {@code null}
     * @return normalized condition expression list
     */
    public static List<String> collect(
            Conditions conditions,
            Range range,
            Min min,
            Max max,
            Length length,
            Regex regex
    ) {
        List<String> out = new ArrayList<>(CommandConditionExpressionNormalizer.normalize(conditions));
        if (range != null) {
            out.add("range:min=" + range.min() + ",max=" + range.max());
        }
        if (min != null) {
            out.add("min:value=" + min.value());
        }
        if (max != null) {
            out.add("max:value=" + max.value());
        }
        if (length != null) {
            out.add("length:min=" + length.min() + ",max=" + length.max());
        }
        if (regex != null) {
            out.add("regex:pattern=" + regex.value());
        }
        return out;
    }
}
