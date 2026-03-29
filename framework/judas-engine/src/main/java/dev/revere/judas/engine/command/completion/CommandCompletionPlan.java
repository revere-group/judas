package dev.revere.judas.engine.command.completion;

import dev.revere.judas.model.command.ParameterDescriptor;

import java.util.Collections;
import java.util.List;

/**
 * Immutable completion target produced by {@link CommandCompletionPlanner}.
 *
 * <p>A plan represents exactly one target kind:
 * parameter completion, option-alias completion, or empty/no-op completion.
 */
public final class CommandCompletionPlan {
    private final ParameterDescriptor targetParameter;
    private final String partial;
    private final List<String> optionAliases;

    private CommandCompletionPlan(ParameterDescriptor targetParameter, String partial, List<String> optionAliases) {
        this.targetParameter = targetParameter;
        this.partial = partial;
        this.optionAliases = optionAliases;
    }

    /**
     * Creates a parameter-target completion plan.
     *
     * @param parameter parameter being completed
     * @param partial partial token value
     * @return parameter completion plan
     */
    public static CommandCompletionPlan forParameter(ParameterDescriptor parameter, String partial) {
        return new CommandCompletionPlan(parameter, partial, Collections.<String>emptyList());
    }

    /**
     * Creates an option-alias completion plan.
     *
     * @param partial option prefix being completed
     * @param aliases available option aliases
     * @return option-alias completion plan
     */
    public static CommandCompletionPlan forOptionAliases(String partial, List<String> aliases) {
        return new CommandCompletionPlan(null, partial, aliases);
    }

    /**
     * Creates an empty completion plan.
     *
     * @return empty completion plan
     */
    public static CommandCompletionPlan empty() {
        return new CommandCompletionPlan(null, "", Collections.<String>emptyList());
    }

    /**
     * @return {@code true} when a parameter value should be completed
     */
    public boolean targetsParameter() {
        return targetParameter != null;
    }

    /**
     * @return {@code true} when option aliases should be completed
     */
    public boolean targetsOptionAliases() {
        return targetParameter == null && !optionAliases.isEmpty();
    }

    /**
     * @return target parameter, or {@code null}
     */
    public ParameterDescriptor getTargetParameter() {
        return targetParameter;
    }

    /**
     * @return partial token prefix
     */
    public String getPartial() {
        return partial;
    }

    /**
     * @return option aliases to filter, or empty list
     */
    public List<String> getOptionAliases() {
        return optionAliases;
    }
}
