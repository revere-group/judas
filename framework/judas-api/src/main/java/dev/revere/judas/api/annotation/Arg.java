package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Logical argument name for this handler slot: positional tokens, values bound via {@link Switch} or
 * {@link Flag}, and derived metadata (defaults, suggestions). Named tokens such as {@code --rank} are declared
 * on {@link Switch} (valued) or {@link Flag} (boolean); this annotation names the argument the framework binds.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    /**
     * @return logical argument name for binding and help
     */
    String value();
}
