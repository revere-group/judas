package dev.revere.judas.engine.command.routing;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds human-readable usage hints for subcommand routing errors.
 */
public final class SubcommandUsageFormatter {

    /**
     * Utility class.
     */
    private SubcommandUsageFormatter() {
    }

    /**
     * Formats the visible subcommand aliases for one root descriptor.
     *
     * @param descriptor root descriptor containing registered subcommands
     * @return comma-separated subcommand alias list
     */
    public static String formatSubcommandList(CommandDescriptor descriptor) {
        return formatSubcommandList(descriptor, false, "help");
    }

    /**
     * Formats the visible subcommand aliases with optional generated-help alias inclusion.
     *
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
