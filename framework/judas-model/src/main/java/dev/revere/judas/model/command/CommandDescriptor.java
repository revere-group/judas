package dev.revere.judas.model.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable metadata for a registered root command plus mutable runtime handler links.
 */
public class CommandDescriptor {
    private final String[] names;
    private final String permission;
    private final String description;
    private final boolean hidden;
    private final boolean generateHelp;
    private final List<String> conditions;
    private final BaseCommand instance;
    private final List<CommandMethodDescriptor> subcommands = new ArrayList<>();
    private CommandMethodDescriptor defaultMethod;

    /**
     * @param names root aliases
     * @param permission optional permission required for the root handler
     * @param description optional root description
     * @param hidden whether root should be hidden from help output
     * @param instance holder instance owning this root command
     */
    public CommandDescriptor(String[] names, String permission, String description, boolean hidden, BaseCommand instance) {
        this(names, permission, description, hidden, instance, false, Collections.<String>emptyList());
    }

    /**
     * @param names root aliases
     * @param permission optional permission required for the root handler
     * @param description optional root description
     * @param hidden whether root should be hidden from help output
     * @param instance holder instance owning this root command
     * @param generateHelp whether framework-generated help should be enabled
     */
    public CommandDescriptor(
            String[] names,
            String permission,
            String description,
            boolean hidden,
            BaseCommand instance,
            boolean generateHelp
    ) {
        this(names, permission, description, hidden, instance, generateHelp, Collections.<String>emptyList());
    }

    /**
     * @param names root aliases
     * @param permission optional permission required for the root handler
     * @param description optional root description
     * @param hidden whether root should be hidden from help output
     * @param instance holder instance owning this root command
     * @param generateHelp whether framework-generated help should be enabled
     * @param conditions root-level condition expressions
     */
    public CommandDescriptor(
            String[] names,
            String permission,
            String description,
            boolean hidden,
            BaseCommand instance,
            boolean generateHelp,
            List<String> conditions
    ) {
        this.names = Arrays.copyOf(names, names.length);
        this.permission = permission;
        this.description = description;
        this.hidden = hidden;
        this.generateHelp = generateHelp;
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.instance = instance;
    }

    /**
     * @param subcommand parsed subcommand descriptor
     */
    public void addSubcommand(CommandMethodDescriptor subcommand) {
        this.subcommands.add(subcommand);
    }

    /**
     * @return root aliases
     */
    public String[] getNames() {
        return Arrays.copyOf(names, names.length);
    }

    /**
     * @return permission node, or {@code null}
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return root description, or {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return {@code true} when hidden from help output
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @return {@code true} when this root supports generated help subcommand output
     */
    public boolean isGenerateHelp() {
        return generateHelp;
    }

    /**
     * @return immutable root-level condition expression list
     */
    public List<String> getConditions() {
        return conditions;
    }

    /**
     * @return holder instance owning this root command
     */
    public BaseCommand getInstance() {
        return instance;
    }

    /**
     * @return immutable subcommand descriptor list
     */
    public List<CommandMethodDescriptor> getSubcommands() {
        return Collections.unmodifiableList(subcommands);
    }

    /**
     * @return default method descriptor for root execution fallback
     */
    public CommandMethodDescriptor getDefaultMethod() {
        return defaultMethod;
    }

    /**
     * @param defaultMethod default handler descriptor for root execution
     */
    public void setDefaultMethod(CommandMethodDescriptor defaultMethod) {
        this.defaultMethod = defaultMethod;
    }
}
