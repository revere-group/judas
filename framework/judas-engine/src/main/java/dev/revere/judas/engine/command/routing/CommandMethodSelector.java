package dev.revere.judas.engine.command.routing;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.resolver.ArgumentBuffer;

import java.util.List;

/**
 * Selects which command handler should run for the buffered input.
 */
public final class CommandMethodSelector {

    /**
     * Resolves the handler to execute for the current argument buffer.
     *
     * @param descriptor root command descriptor
     * @param arguments mutable argument buffer
     * @return selection result describing match/no-handler/unknown-subcommand states
     */
    public CommandMethodSelection select(CommandDescriptor descriptor, ArgumentBuffer arguments) {
        List<CommandMethodDescriptor> subcommands = descriptor.getSubcommands();
        if (subcommands.isEmpty()) {
            return CommandMethodSelection.matched(descriptor.getDefaultMethod());
        }

        if (!arguments.hasNext()) {
            CommandMethodDescriptor fallback = descriptor.getDefaultMethod();
            if (fallback == null) {
                return CommandMethodSelection.noHandler();
            }
            return CommandMethodSelection.matched(fallback);
        }

        String candidate = arguments.peek();
        for (CommandMethodDescriptor subcommand : subcommands) {
            for (String name : subcommand.getNames()) {
                if (name.equalsIgnoreCase(candidate)) {
                    arguments.consume();
                    return CommandMethodSelection.matched(subcommand);
                }
            }
        }

        return CommandMethodSelection.unknownSubcommand(candidate);
    }
}
