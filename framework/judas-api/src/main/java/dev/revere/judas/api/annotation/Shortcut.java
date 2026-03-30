package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more shortcut root aliases for a subcommand handler method.
 *
 * <p>When used with {@link Subcommand}, the method remains a subcommand of its owning root
 * while also being invokable directly via the shortcut alias.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Shortcut {

    /**
     * @return shortcut root aliases
     */
    String[] names();

    /**
     * @return {@code true} to hide from generic help listings
     */
    boolean hidden() default false;
}
