package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a tab-completion provider type to a parameter.
 * The value must implement {@code SuggestionProvider} from the judas-model module and declare a public no-arg
 * constructor (unless pre-registered on the command manager).
 *
 * <p>You can also provide small static suggestion sets directly using {@link #literals()} to avoid creating a
 * dedicated provider class for simple cases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Suggestions {

    /**
     * @return suggestion provider implementation class, or {@link Void} when using only literals
     */
    Class<?> value() default Void.class;

    /**
     * @return static literal suggestions for simple parameters
     */
    String[] literals() default {};
}
