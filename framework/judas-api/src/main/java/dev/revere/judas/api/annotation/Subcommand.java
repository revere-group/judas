package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a subcommand handler under a root command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {

    /**
     * @return subcommand aliases
     */
    String[] names();

    /**
     * Optional explicit root command name this subcommand belongs to when the holder declares multiple roots.
     *
     * @return root command alias, or empty for automatic linking
     */
    String parent() default "";

    /**
     * @return {@code true} to hide from generic help listings
     */
    boolean hidden() default false;
}
