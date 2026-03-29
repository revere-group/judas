package dev.revere.judas.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a tab-completion provider type to a parameter.
 * The value must implement {@code SuggestionProvider} from the judas-model module and declare a public no-arg
 * constructor (unless pre-registered on the command manager).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Suggestions {

    /**
     * @return suggestion provider implementation class
     */
    Class<?> value();
}
