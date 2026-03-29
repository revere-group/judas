package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more condition expressions to evaluate before command execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface Conditions {

    /**
     * @return condition expressions (for example {@code "player-only"} or {@code "range:min=1,max=5"})
     */
    String[] value();
}
