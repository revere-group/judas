package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric parameter is within an inclusive range.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Range {
    /**
     * @return inclusive minimum value
     */
    double min() default -Double.MAX_VALUE;

    /**
     * @return inclusive maximum value
     */
    double max() default Double.MAX_VALUE;
}
