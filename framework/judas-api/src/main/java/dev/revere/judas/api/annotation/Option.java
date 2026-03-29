package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares explicit option aliases for a value parameter (for example {@code --skin} or {@code -k}).
 * When omitted, a long alias is derived from {@link Name} as {@code --name}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Option {

    /**
     * @return explicit option aliases
     */
    String[] names() default {};

    /**
     * @return {@code true} if this parameter may still be consumed positionally when not passed as an option
     */
    boolean positional() default true;
}
