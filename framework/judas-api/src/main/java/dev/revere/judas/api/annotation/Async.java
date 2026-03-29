package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks command handlers to run on the configured async executor.
 *
 * <p>Any non-void return value still flows through the configured command response handlers after the async work
 * completes, so runtimes should treat both the handler body and response handling as off-thread work.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Async {
}
