package dev.revere.judas.engine.command.routing;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.resolver.ArgumentBuffer;
import dev.revere.judas.model.spi.CommandExecutionServices;

/**
 * Detects and renders the generated help subcommand for one root command.
 */
public final class GeneratedHelpRequestHandler {
    private final CommandExecutionServices executionServices;

    /**
     * Creates a generated-help detector bound to runtime help services.
     *
     * @param executionServices runtime help services
     */
    public GeneratedHelpRequestHandler(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Handles one invocation when the first token maps to the configured help alias.
     *
     * @param descriptor root descriptor
     * @param arguments mutable argument buffer
     * @param context execution context
     * @return {@code true} when the request was handled as generated help
     */
    public boolean handleIfRequested(CommandDescriptor descriptor, ArgumentBuffer arguments, CommandContext context) {
        if (!descriptor.isGenerateHelp() || !arguments.hasNext()) {
            return false;
        }
        String requested = arguments.peek();
        if (!requested.equalsIgnoreCase(this.executionServices.getHelpSubcommandName())) {
            return false;
        }
        arguments.consume();
        for (String line : this.executionServices.getHelpFormatter().renderGeneratedHelp(descriptor)) {
            context.sendMessage(CommandColor.INFO, line);
        }
        return true;
    }
}
