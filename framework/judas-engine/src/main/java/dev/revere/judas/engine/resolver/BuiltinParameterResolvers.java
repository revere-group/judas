package dev.revere.judas.engine.resolver;

import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolveContext;
import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Registers primitive and common string resolvers used by every runtime.
 */
public final class BuiltinParameterResolvers {

    private BuiltinParameterResolvers() {
    }

    public static void registerAll(ParameterResolverRegistry registry) {
        registry.register(String.class, new ParameterResolver<String>() {
            @Override
            public String resolve(ParameterResolveContext context) {
                if (context.getParameter().isConsumeRemaining()) {
                    return context.getArguments().consumeRemaining(" ");
                }
                return ArgumentTokenReader.requireNext(context);
            }

            @Override
            public List<String> complete(CompletionContext context) {
                return new ArrayList<>();
            }
        });
        registry.register(Integer.class, BuiltinParameterResolvers::parseIntToken);
        registry.register(int.class, BuiltinParameterResolvers::parseIntToken);
        registry.register(Double.class, BuiltinParameterResolvers::parseDoubleToken);
        registry.register(double.class, BuiltinParameterResolvers::parseDoubleToken);
        registry.register(Float.class, BuiltinParameterResolvers::parseFloatToken);
        registry.register(float.class, BuiltinParameterResolvers::parseFloatToken);
        registry.register(Long.class, BuiltinParameterResolvers::parseLongToken);
        registry.register(long.class, BuiltinParameterResolvers::parseLongToken);

        ParameterResolver<Boolean> booleanBoxed = new ParameterResolver<Boolean>() {
            @Override
            public Boolean resolve(ParameterResolveContext context) {
                return parseBooleanToken(context);
            }

            @Override
            public List<String> complete(CompletionContext context) {
                return booleanLiterals(context.getPartial());
            }
        };
        ParameterResolver<Boolean> booleanPrimitive = new ParameterResolver<Boolean>() {
            @Override
            public Boolean resolve(ParameterResolveContext context) {
                return parseBooleanToken(context);
            }

            @Override
            public List<String> complete(CompletionContext context) {
                return booleanLiterals(context.getPartial());
            }
        };
        registry.register(Boolean.class, booleanBoxed);
        registry.register(boolean.class, booleanPrimitive);
    }

    private static List<String> booleanLiterals(String partial) {
        String p = partial.toLowerCase(Locale.ROOT);
        List<String> literals = Arrays.asList("true", "false");
        if (p.isEmpty()) {
            return new ArrayList<>(literals);
        }
        List<String> out = new ArrayList<>();
        for (String literal : literals) {
            if (literal.startsWith(p)) {
                out.add(literal);
            }
        }
        return out;
    }

    private static Integer parseIntToken(ParameterResolveContext context) {
        return Integer.valueOf(ArgumentTokenReader.requireNext(context));
    }

    private static Double parseDoubleToken(ParameterResolveContext context) {
        return Double.valueOf(ArgumentTokenReader.requireNext(context));
    }

    private static Float parseFloatToken(ParameterResolveContext context) {
        return Float.valueOf(ArgumentTokenReader.requireNext(context));
    }

    private static Long parseLongToken(ParameterResolveContext context) {
        return Long.valueOf(ArgumentTokenReader.requireNext(context));
    }

    private static Boolean parseBooleanToken(ParameterResolveContext context) {
        return Boolean.valueOf(ArgumentTokenReader.requireNext(context));
    }
}
