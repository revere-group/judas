package dev.revere.judas.engine.command.routing;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.engine.command.execution.CommandConditionEvaluator;
import dev.revere.judas.engine.command.execution.CommandCooldownEvaluator;
import dev.revere.judas.engine.command.execution.CommandArgumentBinder;
import dev.revere.judas.engine.command.execution.CommandExecutionCoordinator;
import dev.revere.judas.engine.command.execution.CommandInvocationExecutor;
import dev.revere.judas.engine.command.execution.CommandMethodInvoker;
import dev.revere.judas.engine.command.execution.CommandResponseDispatcher;
import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.spi.CommandExecutionServices;

/**
 * Orchestrates one command invocation from root dispatch through help, guards, and execution.
 *
 * <p>Flow:
 * <ol>
 *     <li>Verify root permission.</li>
 *     <li>Handle generated help requests.</li>
 *     <li>Resolve subcommand/default handler.</li>
 *     <li>Verify handler permission.</li>
 *     <li>Evaluate cooldown and condition guards.</li>
 *     <li>Delegate sync/async invocation to execution collaborators.</li>
 * </ol>
 */
public final class CommandRouter {
    private final CommandExecutionServices executionServices;
    private final CommandMethodSelector methodSelector = new CommandMethodSelector();
    private final GeneratedHelpRequestHandler helpRequestHandler;
    private final CommandConditionEvaluator conditionEvaluator;
    private final CommandCooldownEvaluator cooldownEvaluator;
    private final CommandExecutionCoordinator executionCoordinator;

    /**
     * Creates a router bound to one manager's resolver ecosystem.
     *
     * @param executionServices runtime services providing resolvers and suggestion providers
     */
    public CommandRouter(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
        CommandArgumentBinder argumentBinder = new CommandArgumentBinder(executionServices);
        CommandConditionEvaluator conditionEvaluator = new CommandConditionEvaluator(executionServices);
        this.helpRequestHandler = new GeneratedHelpRequestHandler(executionServices);
        this.conditionEvaluator = conditionEvaluator;
        this.cooldownEvaluator = new CommandCooldownEvaluator(executionServices);
        this.executionCoordinator = new CommandExecutionCoordinator(
                executionServices,
                new CommandInvocationExecutor(
                        executionServices,
                        argumentBinder,
                        new CommandMethodInvoker(),
                        conditionEvaluator
                ),
                new CommandResponseDispatcher(executionServices)
        );
    }

    /**
     * Dispatches one command execution from platform context.
     *
     * @param descriptor root descriptor resolved by platform command mapping
     * @param context sender + arguments + messaging abstraction
     */
    public void dispatch(CommandDescriptor descriptor, CommandContext context) {
        ArgumentBuffer arguments = new ArgumentBuffer(context.getArguments());

        if (descriptor.getPermission() != null && !context.hasPermission(descriptor.getPermission())) {
            context.sendMessage(CommandColor.ERROR, this.executionServices.getMessageProvider().noPermissionForRoot(descriptor));
            return;
        }

        if (this.helpRequestHandler.handleIfRequested(descriptor, arguments, context)) {
            return;
        }

        CommandMethodSelection selection = this.methodSelector.select(descriptor, arguments);
        if (selection.getKind() == CommandMethodSelection.Kind.UNKNOWN_SUBCOMMAND) {
            String available = SubcommandUsageFormatter.formatSubcommandList(
                    descriptor,
                    descriptor.isGenerateHelp(),
                    this.executionServices.getHelpSubcommandName()
            );
            context.sendMessage(CommandColor.ERROR, this.executionServices.getMessageProvider().unknownSubcommand(
                    descriptor,
                    selection.getUnknownToken(),
                    available
            ));
            return;
        }

        CommandMethodDescriptor methodDescriptor = selection.getMethod();
        if (methodDescriptor == null) {
            String available = SubcommandUsageFormatter.formatSubcommandList(
                    descriptor,
                    descriptor.isGenerateHelp(),
                    this.executionServices.getHelpSubcommandName()
            );
            context.sendMessage(CommandColor.ERROR, this.executionServices.getMessageProvider().noHandler(descriptor, available));
            context.sendMessage(CommandColor.INFO, this.executionServices.getHelpFormatter().renderRootUsage(descriptor, available));
            return;
        }

        if (methodDescriptor.getPermission() != null && !context.hasPermission(methodDescriptor.getPermission())) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().noPermissionForSubcommand(descriptor, methodDescriptor)
            );
            return;
        }

        long remainingCooldown = this.cooldownEvaluator.acquireRemaining(descriptor, methodDescriptor, context);
        if (remainingCooldown > 0L) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().cooldownActive(descriptor, methodDescriptor, remainingCooldown)
            );
            return;
        }

        try {
            this.conditionEvaluator.evaluateHandler(descriptor, methodDescriptor, context);
        } catch (CommandConditionException exception) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().conditionError(descriptor, methodDescriptor, exception)
            );
            return;
        }
        this.executionCoordinator.execute(descriptor, methodDescriptor, context, arguments, new CommandExecutionCoordinator.RunnableInvoker() {
            @Override
            public void run(RuntimeException exception) {
                CommandRouter.this.handleInvocationFailure(descriptor, methodDescriptor, context, exception);
            }
        });
    }

    private void handleInvocationFailure(
            CommandDescriptor descriptor,
            CommandMethodDescriptor methodDescriptor,
            CommandContext context,
            RuntimeException exception
    ) {
        if (exception instanceof CommandBindingException) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().bindingError(
                            descriptor,
                            methodDescriptor,
                            (CommandBindingException) exception
                    )
            );
            if (this.executionServices.isShowUsageAfterBindingError()) {
                context.sendMessage(
                        CommandColor.INFO,
                        this.executionServices.getHelpFormatter().renderSubcommandUsage(descriptor, methodDescriptor)
                );
            }
            return;
        }
        context.sendMessage(
                CommandColor.ERROR,
                this.executionServices.getMessageProvider().executionError(descriptor, methodDescriptor, exception)
        );
    }
}
