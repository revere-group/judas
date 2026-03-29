package dev.revere.judas.engine.command;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

/**
 * Factory helpers for producing descriptors derived from parsed commands.
 */
public final class CommandDescriptorBuilder {

    private CommandDescriptorBuilder() {
    }

    /**
     * Rebuilds a descriptor using alternate command names while preserving handler wiring.
     *
     * @param source parsed source descriptor
     * @param names replacement root aliases
     * @return copied descriptor with rewritten names and original handlers
     */
    public static CommandDescriptor withNames(CommandDescriptor source, String... names) {
        CommandDescriptor copy = new CommandDescriptor(
                names,
                source.getPermission(),
                source.getDescription(),
                source.isHidden(),
                source.getInstance(),
                source.isGenerateHelp()
        );

        for (CommandMethodDescriptor subcommand : source.getSubcommands()) {
            copy.addSubcommand(subcommand);
        }

        copy.setDefaultMethod(source.getDefaultMethod());
        return copy;
    }
}
