package dev.revere.judas.engine.command;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds human-readable usage hints for subcommand routing errors.
 */
public final class SubcommandUsageFormatter {

    private SubcommandUsageFormatter() {
    }

    /**
     * @param descriptor root descriptor containing registered subcommands
     * @return comma-separated subcommand alias list
     */
    public static String formatSubcommandList(CommandDescriptor descriptor) {
        return formatSubcommandList(descriptor, false, "help");
    }

    /**
     * @param descriptor root descriptor containing registered subcommands
     * @param includeGeneratedHelp whether generated help alias should be appended
     * @param generatedHelpAlias generated help alias token
     * @return comma-separated subcommand alias list
     */
    public static String formatSubcommandList(
            CommandDescriptor descriptor,
            boolean includeGeneratedHelp,
            String generatedHelpAlias
    ) {
        Set<String> names = new LinkedHashSet<>();
        for (CommandMethodDescriptor subcommand : descriptor.getSubcommands()) {
            Collections.addAll(names, subcommand.getNames());
        }
        if (includeGeneratedHelp && generatedHelpAlias != null && !generatedHelpAlias.trim().isEmpty()) {
            names.add(generatedHelpAlias.trim());
        }

        if (names.isEmpty()) {
            return "";
        }

        return String.join(", ", names);
    }
}
