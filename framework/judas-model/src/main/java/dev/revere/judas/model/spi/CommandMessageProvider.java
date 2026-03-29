package dev.revere.judas.model.spi;

import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandConditionException;

/**
 * Produces framework-side feedback messages for command execution and routing outcomes.
 */
public interface CommandMessageProvider {
    /**
     * @param rootToken unknown root command token
     * @return unknown-root message
     */
    String unknownRootCommand(String rootToken);

    /**
     * @param descriptor root descriptor
     * @return root-level no-permission message
     */
    String noPermissionForRoot(CommandDescriptor descriptor);

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor matched subcommand descriptor
     * @return subcommand-level no-permission message
     */
    String noPermissionForSubcommand(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor);

    /**
     * @param descriptor root descriptor
     * @param token unknown subcommand token
     * @param availableSubcommands comma-separated available subcommand list
     * @return unknown-subcommand message
     */
    String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands);

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor target handler
     * @param remainingMillis cooldown remaining duration
     * @return cooldown-active message
     */
    String cooldownActive(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor, long remainingMillis);

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor target handler
     * @param exception validation/condition failure
     * @return condition failure message
     */
    String conditionError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandConditionException exception
    );

    /**
     * @param descriptor root descriptor
     * @param availableSubcommands comma-separated available subcommand list
     * @return missing-handler/usage message
     */
    String noHandler(CommandDescriptor descriptor, String availableSubcommands);

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor handler being bound
     * @param exception binding failure
     * @return binding failure message
     */
    String bindingError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandBindingException exception
    );

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor handler being executed
     * @param exception runtime execution exception
     * @return generic execution failure message
     */
    String executionError(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            RuntimeException exception
    );
}
