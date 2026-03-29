package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Boolean switch aliases (presence toggles) such as {@code -s} / {@code --silent}. Non-boolean parameters
 * that take a separate value token use {@link Switch}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {

    /**
     * @return explicit flag aliases
     */
    String[] names() default {};
}
