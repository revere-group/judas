package dev.revere.judas.engine.command.completion;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.engine.command.input.CommandInputParser;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.command.ParsedCommandInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computes a completion plan for one handler and current input snapshot.
 *
 * <p>The planner does not resolve concrete suggestions itself. It only selects the target:
 * pending option value, option alias list, or positional parameter.
 */
public final class CommandCompletionPlanner {
    /**
     * Builds the next completion plan for the provided handler input.
     *
     * @param handler target handler
     * @param args raw tokens for that handler
     * @return completion plan describing the next completion target
     */
    public CommandCompletionPlan plan(CommandMethodDescriptor handler, String[] args) {
        if (handler == null) {
            return CommandCompletionPlan.empty();
        }

        List<ParameterDescriptor> consumable = this.collectConsumableParameters(handler);
        if (consumable.isEmpty()) {
            return CommandCompletionPlan.empty();
        }

        ParsedCommandInput parsedInput = CommandInputParser.parseForCompletion(args, consumable);
        if (parsedInput.getPendingValueParameter() != null) {
            return CommandCompletionPlan.forParameter(
                    parsedInput.getPendingValueParameter(),
                    parsedInput.getPendingValuePrefix()
            );
        }
        if (parsedInput.getOptionPrefix() != null) {
            return CommandCompletionPlan.forOptionAliases(
                    parsedInput.getOptionPrefix(),
                    this.availableOptionAliases(consumable, parsedInput)
            );
        }
        return this.planForPositionalTokens(consumable, parsedInput);
    }

    /**
     * Collects parameters that consume user tokens.
     *
     * @param handler target handler
     * @return consumable parameter descriptors
     */
    private List<ParameterDescriptor> collectConsumableParameters(CommandMethodDescriptor handler) {
        List<ParameterDescriptor> out = new ArrayList<>();
        for (ParameterDescriptor parameter : handler.getParameters()) {
            if (CommandContext.class.isAssignableFrom(parameter.getType())) {
                continue;
            }
            if (parameter.isSenderInjection()) {
                continue;
            }
            out.add(parameter);
        }
        return out;
    }

    /**
     * Plans completion target for positional token flow after named options are applied.
     *
     * @param parameters handler parameters
     * @param parsedInput parsed input snapshot
     * @return positional completion plan
     */
    private CommandCompletionPlan planForPositionalTokens(
            List<ParameterDescriptor> parameters,
            ParsedCommandInput parsedInput
    ) {
        List<ParameterDescriptor> positionalParameters = this.remainingPositionalParameters(parameters, parsedInput);
        if (positionalParameters.isEmpty()) {
            return CommandCompletionPlan.empty();
        }

        List<String> positionalTokens = parsedInput.getPositionalTokens();
        int count = positionalTokens.size();
        if (count == 0) {
            return CommandCompletionPlan.forParameter(positionalParameters.get(0), "");
        }

        int maxParameters = positionalParameters.size();
        ParameterDescriptor lastParameter = positionalParameters.get(maxParameters - 1);
        if (count > maxParameters && !lastParameter.isConsumeRemaining()) {
            return CommandCompletionPlan.empty();
        }

        ParameterDescriptor target = count > maxParameters ? lastParameter : positionalParameters.get(count - 1);
        return CommandCompletionPlan.forParameter(target, positionalTokens.get(count - 1));
    }

    /**
     * Returns positional parameters still eligible for token consumption.
     *
     * @param parameters all consumable parameters
     * @param parsedInput parsed input snapshot
     * @return remaining positional parameters
     */
    private List<ParameterDescriptor> remainingPositionalParameters(
            List<ParameterDescriptor> parameters,
            ParsedCommandInput parsedInput
    ) {
        List<ParameterDescriptor> result = new ArrayList<>();
        for (ParameterDescriptor parameter : parameters) {
            if (parameter.isFlag()) {
                continue;
            }
            if (parsedInput.hasNamedValue(parameter)) {
                continue;
            }
            if (!parameter.isPositionalAllowed()) {
                continue;
            }
            result.add(parameter);
        }
        return result;
    }

    /**
     * Returns option/flag aliases that can still be suggested.
     *
     * @param parameters all consumable parameters
     * @param parsedInput parsed input snapshot
     * @return remaining option aliases
     */
    private List<String> availableOptionAliases(List<ParameterDescriptor> parameters, ParsedCommandInput parsedInput) {
        List<String> aliases = new ArrayList<>();
        for (ParameterDescriptor parameter : parameters) {
            if (parameter.isFlag() && parsedInput.hasFlag(parameter)) {
                continue;
            }
            if (!parameter.isFlag() && parsedInput.hasNamedValue(parameter)) {
                continue;
            }
            Collections.addAll(aliases, parameter.getOptionNames());
        }
        return aliases;
    }
}
