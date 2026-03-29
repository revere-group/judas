package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes annotation-declared condition expressions into immutable string lists.
 */
public final class CommandConditionExpressionNormalizer {
    private CommandConditionExpressionNormalizer() {
    }

    /**
     * Normalizes one {@link Conditions} annotation into an immutable expression list.
     *
     * @param annotation conditions annotation, or {@code null}
     * @return normalized immutable condition list
     */
    public static List<String> normalize(Conditions annotation) {
        if (annotation == null || annotation.value().length == 0) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String raw : annotation.value()) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            out.add(raw.trim());
        }
        return Collections.unmodifiableList(out);
    }
}
