package dev.revere.judas.model.spi;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;

import java.util.List;

/**
 * Renders generated help output and concise usage hints.
 */
public interface CommandHelpFormatter {

    /**
     * @param descriptor root descriptor
     * @return detailed help lines to send when generated help is requested
     */
    List<String> renderGeneratedHelp(CommandDescriptor descriptor);

    /**
     * @param descriptor root descriptor
     * @param availableSubcommands comma-separated available subcommand list
     * @return concise root usage hint
     */
    String renderRootUsage(CommandDescriptor descriptor, String availableSubcommands);

    /**
     * @param descriptor root descriptor
     * @param methodDescriptor handler descriptor
     * @return concise usage hint for a specific subcommand handler
     */
    String renderSubcommandUsage(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor);
}
