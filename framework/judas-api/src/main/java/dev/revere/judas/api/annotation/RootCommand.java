package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a root command name list (and optional visibility) on a class or handler method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RootCommand {

    /**
     * @return primary command aliases
     */
    String[] names();

    /**
     * @return {@code true} to hide from generic help listings
     */
    boolean hidden() default false;

    /**
     * When enabled, the framework auto-exposes a generated {@code help} subcommand for this root command.
     *
     * @return {@code true} to generate built-in help for this root
     */
    boolean generateHelp() default false;
}
