package dev.revere.judas.runtime;

import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.condition.ConditionContext;
import dev.revere.judas.model.condition.ConditionRegistry;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Registers built-in validation conditions derived from parameter annotations.
 */
public final class BuiltinCommandConditions {
    private BuiltinCommandConditions() {
    }

    public static void registerAll(ConditionRegistry registry) {
        registry.register("range", context -> {
            Number number = requireNumber(context, "range");
            Map<String, String> args = parseArgs(context.getExpression());
            double min = parseDouble(args, "min", -Double.MAX_VALUE);
            double max = parseDouble(args, "max", Double.MAX_VALUE);
            double value = number.doubleValue();
            if (value < min || value > max) {
                throw new CommandConditionException("Value for '" + parameterName(context) + "' must be between " + min + " and " + max + ".");
            }
        });
        registry.register("min", context -> {
            Number number = requireNumber(context, "min");
            Map<String, String> args = parseArgs(context.getExpression());
            double min = parseDouble(args, "value", -Double.MAX_VALUE);
            if (number.doubleValue() < min) {
                throw new CommandConditionException("Value for '" + parameterName(context) + "' must be at least " + min + ".");
            }
        });
        registry.register("max", context -> {
            Number number = requireNumber(context, "max");
            Map<String, String> args = parseArgs(context.getExpression());
            double max = parseDouble(args, "value", Double.MAX_VALUE);
            if (number.doubleValue() > max) {
                throw new CommandConditionException("Value for '" + parameterName(context) + "' must be at most " + max + ".");
            }
        });
        registry.register("length", context -> {
            Object value = context.getParameterValue();
            if (value == null) {
                return;
            }
            int length = lengthOf(value);
            Map<String, String> args = parseArgs(context.getExpression());
            int min = (int) parseDouble(args, "min", 0.0D);
            int max = (int) parseDouble(args, "max", (double) Integer.MAX_VALUE);
            if (length < min || length > max) {
                throw new CommandConditionException("Length for '" + parameterName(context) + "' must be between " + min + " and " + max + ".");
            }
        });
        registry.register("regex", context -> {
            Object value = context.getParameterValue();
            if (value == null) {
                return;
            }
            if (!(value instanceof CharSequence)) {
                throw new CommandConditionException("Condition 'regex' requires text input.");
            }
            Map<String, String> args = parseArgs(context.getExpression());
            String pattern = args.get("pattern");
            if (pattern == null) {
                throw new CommandConditionException("Condition 'regex' requires a pattern.");
            }
            try {
                if (!Pattern.compile(pattern).matcher(String.valueOf(value)).matches()) {
                    throw new CommandConditionException("Value for '" + parameterName(context) + "' does not match expected format.");
                }
            } catch (PatternSyntaxException exception) {
                throw new CommandConditionException("Invalid regex pattern on parameter '" + parameterName(context) + "'.");
            }
        });
    }

    private static Number requireNumber(ConditionContext context, String key) {
        Object value = context.getParameterValue();
        if (value == null) {
            return 0;
        }
        if (!(value instanceof Number)) {
            throw new CommandConditionException("Condition '" + key + "' requires numeric input.");
        }
        return (Number) value;
    }

    private static String parameterName(ConditionContext context) {
        if (context.getParameter() == null) {
            return "value";
        }
        return context.getParameter().getName();
    }

    private static int lengthOf(Object value) {
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        throw new CommandConditionException("Condition 'length' requires text, collection, or array input.");
    }

    private static Map<String, String> parseArgs(String expression) {
        Map<String, String> out = new HashMap<>();
        int split = expression.indexOf(':');
        if (split < 0 || split + 1 >= expression.length()) {
            return out;
        }
        String args = expression.substring(split + 1);
        String[] pairs = args.split(",");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq + 1 >= pair.length()) {
                continue;
            }
            String key = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            if (!key.isEmpty()) {
                out.put(key, value);
            }
        }
        return out;
    }

    private static double parseDouble(Map<String, String> args, String key, double fallback) {
        String value = args.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
