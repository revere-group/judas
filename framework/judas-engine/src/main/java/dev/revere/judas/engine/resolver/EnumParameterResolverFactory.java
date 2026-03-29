package dev.revere.judas.engine.resolver;

import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolveContext;
import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates resolvers for enum parameters.
 */
public final class EnumParameterResolverFactory {

    private EnumParameterResolverFactory() {
    }

    public static ParameterResolver<?> forEnum(Class<?> enumType) {
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException(enumType.getName() + " is not an enum");
        }

        final Object[] constants = enumType.getEnumConstants();

        return new ParameterResolver<Object>() {
            @Override
            public Object resolve(ParameterResolveContext context) {
                String input = ArgumentTokenReader.requireNext(context);
                for (Object constant : constants) {
                    Enum<?> enumConstant = (Enum<?>) constant;
                    if (enumConstant.name().equalsIgnoreCase(input)) {
                        return enumConstant;
                    }
                }
                throw new IllegalArgumentException("Invalid enum value: " + input);
            }

            @Override
            public List<String> complete(CompletionContext context) {
                String partial = context.getPartial().toLowerCase(Locale.ROOT);
                List<String> out = new ArrayList<>();
                for (Object constant : constants) {
                    Enum<?> enumConstant = (Enum<?>) constant;
                    String name = enumConstant.name();
                    if (partial.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                        out.add(name);
                    }
                }
                return out;
            }
        };
    }
}
