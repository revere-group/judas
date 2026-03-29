package dev.revere.judas.model.condition;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;

/**
 * Context passed to command condition implementations.
 */
public final class ConditionContext {
    private final CommandContext commandContext;
    private final CommandDescriptor root;
    private final CommandMethodDescriptor handler;
    private final ParameterDescriptor parameter;
    private final Object parameterValue;
    private final String expression;

    /**
     * Creates immutable condition-evaluation context for one expression.
     *
     * @param commandContext command execution context
     * @param root root descriptor being executed
     * @param handler selected method descriptor
     * @param parameter optional parameter descriptor when condition is parameter-scoped
     * @param parameterValue optional parameter value for parameter-scoped conditions
     * @param expression raw condition expression
     */
    public ConditionContext(
            CommandContext commandContext,
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            ParameterDescriptor parameter,
            Object parameterValue,
            String expression
    ) {
        this.commandContext = commandContext;
        this.root = root;
        this.handler = handler;
        this.parameter = parameter;
        this.parameterValue = parameterValue;
        this.expression = expression;
    }

    /**
     * Returns the invocation context for sender, arguments, and messaging.
     *
     * @return command execution context
     */
    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * Returns the root descriptor active for this invocation.
     *
     * @return root descriptor being executed
     */
    public CommandDescriptor getRoot() {
        return root;
    }

    /**
     * Returns the resolved handler being evaluated.
     *
     * @return selected handler descriptor
     */
    public CommandMethodDescriptor getHandler() {
        return handler;
    }

    /**
     * Returns the parameter currently under validation, when parameter-scoped.
     *
     * @return parameter descriptor when evaluating a parameter condition, otherwise {@code null}
     */
    public ParameterDescriptor getParameter() {
        return parameter;
    }

    /**
     * Returns the bound parameter value currently under validation.
     *
     * @return bound parameter value when evaluating a parameter condition, otherwise {@code null}
     */
    public Object getParameterValue() {
        return parameterValue;
    }

    /**
     * Returns the raw condition expression string after annotation normalization.
     *
     * @return raw condition expression as declared in annotations
     */
    public String getExpression() {
        return expression;
    }
}
