package dev.revere.judas.model.condition;

/**
 * Validates whether a command execution may continue.
 */
@FunctionalInterface
public interface CommandCondition {

    /**
     * Validates one condition expression against the provided invocation context.
     *
     * @param context evaluation context
     * @throws CommandConditionException when condition fails
     */
    void validate(ConditionContext context);
}
