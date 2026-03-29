package dev.revere.judas.engine.command;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.spi.CommandExecutionServices;

/**
 * Routes and executes a command invocation.
 *
 * <p>Flow:
 * <ol>
 *     <li>Verify root permission.</li>
 *     <li>Resolve subcommand/default handler.</li>
 *     <li>Verify handler permission.</li>
 *     <li>Bind arguments and invoke method.</li>
 * </ol>
 */
public class CommandRouter {
    private final CommandExecutionServices executionServices;
    private final CommandMethodSelector methodSelector = new CommandMethodSelector();
    private final CommandArgumentBinder argumentBinder;
    private final CommandMethodInvoker methodInvoker = new CommandMethodInvoker();

    /**
     * Creates a router bound to one manager's resolver ecosystem.
     *
     * @param executionServices runtime services providing resolvers and suggestion providers
     */
    public CommandRouter(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
        this.argumentBinder = new CommandArgumentBinder(executionServices);
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

        if (this.isGeneratedHelpRequest(descriptor, arguments)) {
            for (String line : this.executionServices.getHelpFormatter().renderGeneratedHelp(descriptor)) {
                context.sendMessage(CommandColor.INFO, line);
            }
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

        try {
            Object[] invokeArgs = this.argumentBinder.bind(methodDescriptor, context, arguments);
            this.methodInvoker.invoke(descriptor, methodDescriptor, invokeArgs);
        } catch (CommandBindingException exception) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().bindingError(descriptor, methodDescriptor, exception)
            );
            if (this.executionServices.isShowUsageAfterBindingError()) {
                context.sendMessage(
                        CommandColor.INFO,
                        this.executionServices.getHelpFormatter().renderSubcommandUsage(descriptor, methodDescriptor)
                );
            }
        } catch (RuntimeException exception) {
            context.sendMessage(
                    CommandColor.ERROR,
                    this.executionServices.getMessageProvider().executionError(descriptor, methodDescriptor, exception)
            );
        }
    }

    private boolean isGeneratedHelpRequest(CommandDescriptor descriptor, ArgumentBuffer arguments) {
        if (!descriptor.isGenerateHelp()) {
            return false;
        }
        if (!arguments.hasNext()) {
            return false;
        }
        String requested = arguments.peek();
        if (!requested.equalsIgnoreCase(this.executionServices.getHelpSubcommandName())) {
            return false;
        }
        arguments.consume();
        return true;
    }
}
