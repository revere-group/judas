package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Applies a command cooldown at class or method scope.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cooldown {

    /**
     * @return cooldown duration value (must be positive)
     */
    long value();

    /**
     * @return duration unit
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * @return cooldown scope strategy
     */
    CooldownScope scope() default CooldownScope.SENDER;

    /**
     * Optional custom key suffix used to group cooldown windows.
     *
     * @return custom cooldown key suffix
     */
    String key() default "";
}
