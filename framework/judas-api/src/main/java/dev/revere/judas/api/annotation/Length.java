package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates length constraints for string-like parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Length {
    /**
     * @return inclusive minimum length
     */
    int min() default 0;

    /**
     * @return inclusive maximum length
     */
    int max() default Integer.MAX_VALUE;
}
