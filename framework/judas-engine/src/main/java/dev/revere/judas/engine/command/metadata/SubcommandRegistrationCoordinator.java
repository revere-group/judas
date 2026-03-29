package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.exception.DuplicateCommandException;
import dev.revere.judas.model.exception.SubcommandParentMismatchException;
import dev.revere.judas.model.exception.UnknownRootCommandException;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates subcommand registration workflows so {@code CommandManager} stays orchestration-focused.
 *
 * <p>This coordinator centralizes subcommand-parent compatibility rules, alias collision checks, and selective
 * registration behavior for both explicit-root and annotation-driven parent resolution modes.
 */
public final class SubcommandRegistrationCoordinator {
    private final Map<String, CommandDescriptor> commands;
    private final CommandParser parser;

    /**
     * Creates a coordinator bound to manager root storage and parser metadata.
     *
     * @param commands mutable root-command map from manager
     * @param parser parser used to inspect holders/subcommand metadata
     */
    public SubcommandRegistrationCoordinator(Map<String, CommandDescriptor> commands, CommandParser parser) {
        this.commands = commands;
        this.parser = parser;
    }

    /**
     * Registers one parsed subcommand under an explicit root alias.
     *
     * @param rootName explicit root alias to attach under
     * @param subcommand parsed subcommand descriptor
     */
    public void registerSubcommand(String rootName, CommandMethodDescriptor subcommand) {
        CommandDescriptor root = this.requireRoot(rootName);
        this.addSubcommand(root, subcommand);
    }

    /**
     * Registers all compatible subcommands from holder under one explicit root.
     *
     * @param rootName explicit root alias to attach under
     * @param holder subcommand holder
     */
    public void registerSubcommands(String rootName, BaseCommand holder) {
        CommandDescriptor root = this.requireRoot(rootName);
        for (CommandParser.ParsedSubcommand binding : this.parser.parseSubcommandBindings(holder)) {
            if (!isCompatibleWithExplicitRoot(rootName, binding.getParent())) {
                continue;
            }
            this.addSubcommand(root, binding.getDescriptor());
        }
    }

    /**
     * Registers selected aliases from holder under one explicit root.
     *
     * @param rootName explicit root alias to attach under
     * @param holder subcommand holder
     * @param subcommandAliases aliases to register
     */
    public void registerSubcommands(String rootName, BaseCommand holder, String... subcommandAliases) {
        if (subcommandAliases == null || subcommandAliases.length == 0) {
            throw new IllegalArgumentException("At least one subcommand alias is required.");
        }

        CommandDescriptor root = this.requireRoot(rootName);
        Set<String> requested = normalizeAliases(subcommandAliases);
        Set<String> matched = new LinkedHashSet<>();

        for (CommandParser.ParsedSubcommand binding : this.parser.parseSubcommandBindings(holder)) {
            CommandMethodDescriptor descriptor = binding.getDescriptor();
            if (!matchesAnyAlias(descriptor, requested, matched)) {
                continue;
            }
            if (!isCompatibleWithExplicitRoot(rootName, binding.getParent())) {
                throw new SubcommandParentMismatchException(
                        "Subcommand " + describe(descriptor) + " explicitly targets parent '" + binding.getParent()
                                + "' and cannot be registered under root '" + rootName + "'."
                );
            }
            this.addSubcommand(root, descriptor);
        }

        if (matched.size() != requested.size()) {
            requested.removeAll(matched);
            throw new UnknownRootCommandException(
                    "Subcommand alias(es) not found on " + holder.getClass().getName() + ": " + requested
            );
        }
    }

    /**
     * Registers all subcommands from holder by reading each method's declared parent.
     *
     * @param holder subcommand holder
     */
    public void registerSubcommands(BaseCommand holder) {
        for (CommandParser.ParsedSubcommand binding : this.parser.parseSubcommandBindings(holder)) {
            String parent = binding.getParent();
            if (parent == null || parent.trim().isEmpty()) {
                throw new SubcommandParentMismatchException(
                        "Cannot auto-resolve root for subcommand " + describe(binding.getDescriptor())
                                + " on " + holder.getClass().getName() + ". Add @Subcommand(parent = \"root\")."
                );
            }
            this.registerSubcommand(parent, binding.getDescriptor());
        }
    }

    /**
     * Registers selected aliases from holder by reading each method's declared parent.
     *
     * @param holder subcommand holder
     * @param subcommandAliases aliases to register
     */
    public void registerSubcommands(BaseCommand holder, String... subcommandAliases) {
        if (subcommandAliases == null || subcommandAliases.length == 0) {
            throw new IllegalArgumentException("At least one subcommand alias is required.");
        }

        Set<String> requested = normalizeAliases(subcommandAliases);
        Set<String> matched = new LinkedHashSet<>();

        for (CommandParser.ParsedSubcommand binding : this.parser.parseSubcommandBindings(holder)) {
            if (!matchesAnyAlias(binding.getDescriptor(), requested, matched)) {
                continue;
            }

            String parent = binding.getParent();
            if (parent == null || parent.trim().isEmpty()) {
                throw new SubcommandParentMismatchException(
                        "Cannot auto-resolve root for subcommand " + describe(binding.getDescriptor())
                                + " on " + holder.getClass().getName() + ". Add @Subcommand(parent = \"root\")."
                );
            }
            this.registerSubcommand(parent, binding.getDescriptor());
        }

        if (matched.size() != requested.size()) {
            requested.removeAll(matched);
            throw new UnknownRootCommandException(
                    "Subcommand alias(es) not found on " + holder.getClass().getName() + ": " + requested
            );
        }
    }

    /**
     * Resolves a registered root descriptor or throws when missing.
     *
     * @param rootName root alias token from caller input
     * @return registered root descriptor
     */
    private CommandDescriptor requireRoot(String rootName) {
        if (rootName == null || rootName.trim().isEmpty()) {
            throw new IllegalArgumentException("rootName must not be empty");
        }

        CommandDescriptor root = this.commands.get(rootName.toLowerCase(Locale.ROOT));
        if (root == null) {
            throw new UnknownRootCommandException("Root command '" + rootName + "' is not registered.");
        }
        return root;
    }

    /**
     * Adds one subcommand to a root after duplicate-alias validation.
     *
     * @param root owning root descriptor
     * @param subcommand incoming subcommand descriptor
     */
    private void addSubcommand(CommandDescriptor root, CommandMethodDescriptor subcommand) {
        ensureNoSubcommandAliasCollision(root, subcommand);
        root.addSubcommand(subcommand);
    }

    /**
     * Validates that no alias from {@code incoming} already exists on registered subcommands of {@code root}.
     *
     * @param root owning root descriptor
     * @param incoming subcommand candidate being registered
     */
    private static void ensureNoSubcommandAliasCollision(CommandDescriptor root, CommandMethodDescriptor incoming) {
        for (CommandMethodDescriptor existing : root.getSubcommands()) {
            for (String existingAlias : existing.getNames()) {
                for (String incomingAlias : incoming.getNames()) {
                    if (existingAlias.equalsIgnoreCase(incomingAlias)) {
                        throw new DuplicateCommandException(
                                "Subcommand alias '" + incomingAlias + "' is already registered under root '"
                                        + root.getNames()[0] + "'."
                        );
                    }
                }
            }
        }
    }

    /**
     * Normalizes alias input into a lowercase set used for matching.
     *
     * @param aliases raw alias inputs
     * @return normalized unique alias set
     */
    private static Set<String> normalizeAliases(String... aliases) {
        Set<String> set = new LinkedHashSet<>();
        for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) {
                throw new IllegalArgumentException("Subcommand alias must not be empty.");
            }
            set.add(alias.trim().toLowerCase(Locale.ROOT));
        }
        return set;
    }

    /**
     * Checks whether a descriptor exposes any requested alias and records matches.
     *
     * @param descriptor subcommand descriptor to inspect
     * @param requested normalized aliases requested by caller
     * @param matched matched-alias accumulator
     * @return {@code true} when at least one alias matched
     */
    private static boolean matchesAnyAlias(
            CommandMethodDescriptor descriptor,
            Set<String> requested,
            Set<String> matched
    ) {
        boolean matches = false;
        for (String name : descriptor.getNames()) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (requested.contains(normalized)) {
                matched.add(normalized);
                matches = true;
            }
        }
        return matches;
    }

    /**
     * Returns a human-friendly descriptor label used in error messages.
     *
     * @param descriptor subcommand descriptor
     * @return first alias or {@code <unnamed>}
     */
    private static String describe(CommandMethodDescriptor descriptor) {
        String[] names = descriptor.getNames();
        return names.length == 0 ? "<unnamed>" : names[0];
    }

    /**
     * Checks whether a binding's declared parent is compatible with an explicit target root.
     *
     * @param rootName explicit target root alias
     * @param parent declared parent alias from annotation metadata
     * @return {@code true} when registration under {@code rootName} is allowed
     */
    private static boolean isCompatibleWithExplicitRoot(String rootName, String parent) {
        if (parent == null || parent.trim().isEmpty()) {
            return true;
        }
        return parent.trim().equalsIgnoreCase(rootName.trim());
    }
}
