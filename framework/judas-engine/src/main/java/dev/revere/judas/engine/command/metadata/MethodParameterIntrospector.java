package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.DefaultValue;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Length;
import dev.revere.judas.api.annotation.Max;
import dev.revere.judas.api.annotation.Min;
import dev.revere.judas.api.annotation.Arg;
import dev.revere.judas.api.annotation.Switch;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.ParameterDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link ParameterDescriptor} lists from reflective method metadata.
 */
public final class MethodParameterIntrospector {

    /**
     * Utility class.
     */
    private MethodParameterIntrospector() {
    }

    /**
     * Describes all parameters declared on a command handler method.
     *
     * @param method command handler method
     * @return ordered parameter descriptors matching method signature order
     */
    public static List<ParameterDescriptor> describeParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterDescriptor> result = new ArrayList<>(parameters.length);

        for (Parameter parameter : parameters) {
            Arg arg = parameter.getAnnotation(Arg.class);
            Optional optionalAnnotation = parameter.getAnnotation(Optional.class);
            DefaultValue defaultValue = parameter.getAnnotation(DefaultValue.class);
            ConsumeRemaining consumeRemaining = parameter.getAnnotation(ConsumeRemaining.class);
            Sender sender = parameter.getAnnotation(Sender.class);
            Suggestions suggestions = parameter.getAnnotation(Suggestions.class);
            Switch valueSwitch = parameter.getAnnotation(Switch.class);
            Flag flag = parameter.getAnnotation(Flag.class);
            Conditions conditions = parameter.getAnnotation(Conditions.class);
            Range range = parameter.getAnnotation(Range.class);
            Min min = parameter.getAnnotation(Min.class);
            Max max = parameter.getAnnotation(Max.class);
            Length length = parameter.getAnnotation(Length.class);
            Regex regex = parameter.getAnnotation(Regex.class);

            ParameterAnnotationConsistencyChecker.validate(
                    method,
                    parameter,
                    sender,
                    consumeRemaining,
                    suggestions,
                    valueSwitch,
                    flag
            );
            boolean isFlag = flag != null;
            ParameterSuggestionMetadata suggestionMetadata =
                    ParameterSuggestionMetadataResolver.resolve(method, parameter, suggestions);

            boolean positionalAllowed = valueSwitch == null || valueSwitch.positional();
            String[] optionNames = new String[0];
            if (sender == null && !CommandContext.class.isAssignableFrom(parameter.getType())) {
                String[] explicitNames;
                if (valueSwitch != null) {
                    explicitNames = valueSwitch.names();
                } else if (flag != null) {
                    explicitNames = flag.names();
                } else {
                    explicitNames = new String[0];
                }
                optionNames = ParameterOptionNameResolver.resolve(
                        arg != null ? arg.value() : parameter.getName(),
                        explicitNames
                );
            }

            List<String> allConditions = ParameterValidationConditionCollector.collect(
                    conditions,
                    range,
                    min,
                    max,
                    length,
                    regex
            );

            result.add(new ParameterDescriptor(
                    arg != null ? arg.value() : parameter.getName(),
                    parameter.getType(),
                    optionalAnnotation != null,
                    defaultValue != null ? defaultValue.value() : null,
                    consumeRemaining != null,
                    sender != null,
                    suggestionMetadata.getProviderType(),
                    suggestionMetadata.getInlineSuggestions(),
                    isFlag,
                    positionalAllowed,
                    optionNames,
                    allConditions
            ));
        }

        return result;
    }
}
