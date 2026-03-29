package dev.revere.judas.runtime;

import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.spi.CommandHelpFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Default help/usage rendering used by generated help and fallback usage prompts.
 */
public final class DefaultCommandHelpFormatter implements CommandHelpFormatter {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> renderGeneratedHelp(CommandDescriptor descriptor) {
        List<String> lines = new ArrayList<>();
        String root = descriptor.getNames()[0];
        lines.add("Command: /" + root);
        if (descriptor.getDescription() != null && !descriptor.getDescription().trim().isEmpty()) {
            lines.add("Description: " + descriptor.getDescription());
        }
        lines.add("Aliases: " + String.join(", ", descriptor.getNames()));

        if (descriptor.getDefaultMethod() != null) {
            lines.add("Usage: " + this.renderSubcommandUsage(descriptor, descriptor.getDefaultMethod()));
        }

        if (descriptor.getSubcommands().isEmpty()) {
            lines.add("Subcommands: none");
            return lines;
        }

        lines.add("Subcommands:");
        for (CommandMethodDescriptor subcommand : descriptor.getSubcommands()) {
            if (subcommand.isHidden()) {
                continue;
            }
            String usage = this.renderSubcommandUsage(descriptor, subcommand);
            if (subcommand.getDescription() != null && !subcommand.getDescription().trim().isEmpty()) {
                lines.add("- " + usage + " : " + subcommand.getDescription());
            } else {
                lines.add("- " + usage);
            }
        }
        return lines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String renderRootUsage(CommandDescriptor descriptor, String availableSubcommands) {
        if (availableSubcommands != null && !availableSubcommands.isEmpty()) {
            return "Usage: /" + descriptor.getNames()[0] + " <" + availableSubcommands + ">";
        }
        CommandMethodDescriptor defaultMethod = descriptor.getDefaultMethod();
        if (defaultMethod == null) {
            return "Usage: /" + descriptor.getNames()[0];
        }
        return "Usage: " + this.renderSubcommandUsage(descriptor, defaultMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String renderSubcommandUsage(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor) {
        String root = descriptor.getNames()[0];
        StringBuilder usage = new StringBuilder("/").append(root);

        if (!isDefaultMethod(descriptor, methodDescriptor)) {
            usage.append(" ").append(methodDescriptor.getNames()[0]);
        }

        for (ParameterDescriptor parameter : methodDescriptor.getParameters()) {
            if (parameter.isSenderInjection() || CommandContext.class.isAssignableFrom(parameter.getType())) {
                continue;
            }

            if (parameter.isFlag()) {
                String[] names = parameter.getOptionNames();
                if (names.length > 0) {
                    usage.append(" [").append(names[0]).append("]");
                }
                continue;
            }

            if (parameter.getOptionNames().length > 0 && !parameter.isPositionalAllowed()) {
                usage.append(" ").append(parameter.getOptionNames()[0]).append(" <").append(parameter.getName()).append(">");
                continue;
            }

            if (parameter.isOptional() || parameter.getDefaultValue() != null) {
                usage.append(" [").append(parameter.getName()).append("]");
            } else {
                usage.append(" <").append(parameter.getName()).append(">");
            }
        }
        return usage.toString();
    }

    private static boolean isDefaultMethod(CommandDescriptor descriptor, CommandMethodDescriptor candidate) {
        return descriptor.getDefaultMethod() == candidate;
    }
}
