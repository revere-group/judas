package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valued argument: declares CLI switch tokens (for example {@code --priority} or {@code -p}) that supply
 * this parameter's value in addition to, or instead of, the positional stream. The parameter remains the
 * same logical {@link Arg}; this annotation only adds named entry points. Boolean on/off toggles use
 * {@link Flag} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Switch {

    /**
     * @return explicit switch aliases (long or short forms)
     */
    String[] names() default {};

    /**
     * @return {@code true} if this parameter may still be consumed positionally when not passed via a switch
     */
    boolean positional() default true;
}
