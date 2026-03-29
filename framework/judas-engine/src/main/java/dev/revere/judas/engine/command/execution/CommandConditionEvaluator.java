package dev.revere.judas.engine.command.execution;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.condition.CommandCondition;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.condition.ConditionContext;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.List;

/**
 * Evaluates condition expressions for a command invocation.
 *
 * <p>This evaluator is intentionally stage-aware:
 * root-level and handler-level conditions are validated before argument binding is considered successful,
 * while parameter-level conditions are validated after arguments are resolved to typed values.
 *
 * <p>Condition expressions use the format {@code key[:arguments]} where only {@code key} is used for registry lookup.
 * The full original expression is still forwarded in {@link ConditionContext} so condition implementations may parse
 * their own argument payload.
 */
public final class CommandConditionEvaluator {
    private final CommandExecutionServices executionServices;

    /**
     * Creates a condition evaluator backed by runtime execution services.
     *
     * @param executionServices runtime service container used to resolve registered conditions
     */
    public CommandConditionEvaluator(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Evaluates root-level and handler-level conditions.
     *
     * <p>This method does not inspect parameter conditions; use {@link #evaluateParameters(CommandDescriptor,
     * CommandMethodDescriptor, CommandContext, Object[])} after binding to evaluate those.
     *
     * @param descriptor matched root command descriptor
     * @param methodDescriptor matched handler descriptor
     * @param context command execution context
     * @throws CommandConditionException when a condition key is unknown or a condition rejects execution
     */
    public void evaluateHandler(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, CommandContext context) {
        this.evaluateExpressions(descriptor.getConditions(), descriptor, methodDescriptor, context, null, null);
        this.evaluateExpressions(methodDescriptor.getConditions(), descriptor, methodDescriptor, context, null, null);
    }

    /**
     * Evaluates parameter-level conditions for already-resolved invocation arguments.
     *
     * <p>Each condition receives both the {@link ParameterDescriptor} and its resolved value in the
     * {@link ConditionContext}. This enables value-aware rules such as numeric range, regex, or custom guards.
     *
     * @param descriptor matched root command descriptor
     * @param methodDescriptor matched handler descriptor
     * @param context command execution context
     * @param invokeArgs resolved invocation arguments in method parameter order
     * @throws CommandConditionException when a condition key is unknown or a condition rejects execution
     */
    public void evaluateParameters(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            Object[] invokeArgs
    ) {
        List<ParameterDescriptor> parameters = methodDescriptor.getParameters();
        for (int i = 0; i < parameters.size() && i < invokeArgs.length; i++) {
            ParameterDescriptor parameter = parameters.get(i);
            if (parameter.getConditions().isEmpty()) {
                continue;
            }
            this.evaluateExpressions(
                    parameter.getConditions(),
                    descriptor,
                    methodDescriptor,
                    context,
                    parameter,
                    invokeArgs[i]
            );
        }
    }

    /**
     * Resolves and evaluates a list of condition expressions against one evaluation scope.
     *
     * @param expressions condition expressions to evaluate
     * @param descriptor matched root command descriptor
     * @param methodDescriptor matched handler descriptor
     * @param context command execution context
     * @param parameterDescriptor parameter descriptor for parameter-level evaluation, otherwise {@code null}
     * @param parameterValue resolved parameter value for parameter-level evaluation, otherwise {@code null}
     * @throws CommandConditionException when a condition key is unknown or a condition rejects execution
     */
    private void evaluateExpressions(
            List<String> expressions,
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            ParameterDescriptor parameterDescriptor,
            Object parameterValue
    ) {
        for (String expression : expressions) {
            String key = this.conditionKey(expression);
            CommandCondition condition = this.executionServices.getConditionRegistry().get(key);
            if (condition == null) {
                throw new CommandConditionException("Unknown condition: " + key);
            }
            condition.validate(new ConditionContext(
                    context,
                    descriptor,
                    methodDescriptor,
                    parameterDescriptor,
                    parameterValue,
                    expression
            ));
        }
    }

    /**
     * Extracts the condition registry key from an expression.
     *
     * <p>Examples:
     * {@code "min:value=5" -> "min"},
     * {@code "player-only" -> "player-only"}.
     *
     * @param expression full condition expression
     * @return normalized condition key used for registry lookup
     */
    private String conditionKey(String expression) {
        int split = expression.indexOf(':');
        if (split <= 0) {
            return expression.trim();
        }
        return expression.substring(0, split).trim();
    }
}
