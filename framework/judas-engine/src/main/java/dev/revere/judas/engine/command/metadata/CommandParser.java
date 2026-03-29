package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.RootCommand;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.exception.DuplicateCommandException;
import dev.revere.judas.model.exception.SubcommandParentMismatchException;
import dev.revere.judas.model.exception.SubcommandShortcutAliasConflictException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses command holder classes into root/subcommand descriptor graphs.
 */
public class CommandParser {

    /**
     * Parses exactly one root descriptor from a holder and fails on ambiguity.
     *
     * Parses a holder expected to define exactly one root.
     *
     * @param instance command holder instance
     * @return single root descriptor
     */
    public CommandDescriptor parse(BaseCommand instance) {
        List<CommandDescriptor> all = this.parseAll(instance);
        if (all.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one root command for " + instance.getClass().getName()
                            + " but found " + all.size() + ". Use register(...) for each descriptor explicitly."
            );
        }
        return all.get(0);
    }

    /**
     * Parses all root descriptors and attached subcommands from one holder instance.
     *
     * Parses all roots from a holder.
     *
     * @param instance command holder instance
     * @return all root descriptors parsed from class-level/method-level definitions
     */
    public List<CommandDescriptor> parseAll(BaseCommand instance) {
        Class<?> clazz = instance.getClass();
        RootCommand classRootCommand = clazz.getAnnotation(RootCommand.class);
        Permission classPermission = clazz.getAnnotation(Permission.class);
        Description classDescription = clazz.getAnnotation(Description.class);
        Conditions classConditions = clazz.getAnnotation(Conditions.class);

        List<CommandDescriptor> roots = new ArrayList<>();
        Map<String, CommandDescriptor> rootsByAlias = new LinkedHashMap<>();
        Set<String> primaryRootAliases = new LinkedHashSet<>();

        if (classRootCommand != null) {
            CommandDescriptor root = new CommandDescriptor(
                    classRootCommand.names(),
                    classPermission != null ? classPermission.value() : null,
                    classDescription != null ? classDescription.value() : null,
                    classRootCommand.hidden(),
                    instance,
                    classRootCommand.generateHelp(),
                    CommandConditionExpressionNormalizer.normalize(classConditions)
            );
            this.addRoot(roots, rootsByAlias, root);
            this.addPrimaryAliases(primaryRootAliases, classRootCommand.names());
        }

        List<SubcommandBinding> subcommands = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            RootCommand methodRootCommand = method.getAnnotation(RootCommand.class);

            if (methodRootCommand != null) {
                if (subcommand != null) {
                    subcommands.add(new SubcommandBinding(
                            subcommand.parent(),
                            AnnotatedCommandHandlerParser.parseSubcommand(method, subcommand, clazz)
                    ));

                    this.ensureShortcutAliasDoesNotCollideWithPrimary(
                            primaryRootAliases,
                            methodRootCommand.names(),
                            method,
                            clazz
                    );

                    CommandDescriptor shortcut = this.buildMethodRootDescriptor(
                            method,
                            methodRootCommand,
                            classPermission,
                            classDescription,
                            classConditions,
                            instance
                    );
                    this.addRoot(roots, rootsByAlias, shortcut);
                    continue;
                }

                Permission methodPermission = method.getAnnotation(Permission.class);
                Description methodDescription = method.getAnnotation(Description.class);

                String permission = methodPermission != null
                        ? methodPermission.value()
                        : (classPermission != null ? classPermission.value() : null);
                String description = methodDescription != null
                        ? methodDescription.value()
                        : (classDescription != null ? classDescription.value() : null);

                CommandDescriptor methodRoot = new CommandDescriptor(
                        methodRootCommand.names(),
                        permission,
                        description,
                        methodRootCommand.hidden(),
                        instance,
                        methodRootCommand.generateHelp(),
                        CommandConditionExpressionNormalizer.normalize(classConditions)
                );
                methodRoot.setDefaultMethod(AnnotatedCommandHandlerParser.parseDefaultHandler(method, methodRootCommand, clazz));
                this.addRoot(roots, rootsByAlias, methodRoot);
                this.addPrimaryAliases(primaryRootAliases, methodRootCommand.names());
                continue;
            }

            if (subcommand != null) {
                subcommands.add(new SubcommandBinding(
                        subcommand.parent(),
                        AnnotatedCommandHandlerParser.parseSubcommand(method, subcommand, clazz)
                ));
            }
        }

        if (roots.isEmpty()) {
            throw new IllegalArgumentException(
                    "No root command definition found on " + clazz.getName()
                            + ". Add @RootCommand on the class or on at least one method."
            );
        }

        for (SubcommandBinding binding : subcommands) {
            CommandDescriptor owner = this.resolveOwner(binding.parent, roots, rootsByAlias, clazz);
            owner.addSubcommand(binding.descriptor);
        }

        return roots;
    }

    /**
     * Builds a root descriptor from one {@code @RootCommand}-annotated method.
     *
     * <p>Method-level permission and description override class-level values when present.
     *
     * @param method root handler method
     * @param rootCommand method-level root command metadata
     * @param classPermission class-level fallback permission
     * @param classDescription class-level fallback description
     * @param classConditions class-level root conditions
     * @param instance holder instance backing reflective invocation
     * @return root descriptor whose default method points to {@code method}
     */
    private CommandDescriptor buildMethodRootDescriptor(
            Method method,
            RootCommand rootCommand,
            Permission classPermission,
            Description classDescription,
            Conditions classConditions,
            BaseCommand instance
    ) {
        Permission methodPermission = method.getAnnotation(Permission.class);
        Description methodDescription = method.getAnnotation(Description.class);

        String permission = methodPermission != null
                ? methodPermission.value()
                : (classPermission != null ? classPermission.value() : null);
        String description = methodDescription != null
                ? methodDescription.value()
                : (classDescription != null ? classDescription.value() : null);

        CommandDescriptor descriptor = new CommandDescriptor(
                rootCommand.names(),
                permission,
                description,
                rootCommand.hidden(),
                instance,
                rootCommand.generateHelp(),
                CommandConditionExpressionNormalizer.normalize(classConditions)
        );
        descriptor.setDefaultMethod(AnnotatedCommandHandlerParser.parseDefaultHandler(method, rootCommand, instance.getClass()));
        return descriptor;
    }

    /**
     * Parses only subcommand descriptors from a holder without building roots.
     *
     * Parses subcommand method descriptors only (ignores root definitions).
     *
     * @param instance command holder instance
     * @return parsed subcommand descriptors
     */
    public List<CommandMethodDescriptor> parseSubcommands(BaseCommand instance) {
        List<CommandMethodDescriptor> result = new ArrayList<>();
        for (ParsedSubcommand binding : this.parseSubcommandBindings(instance)) {
            result.add(binding.getDescriptor());
        }
        return result;
    }

    /**
     * Parses subcommands and keeps declared parent aliases for later linking.
     *
     * Parses subcommands with their declared parent binding.
     *
     * @param instance command holder instance
     * @return parsed subcommand bindings
     */
    public List<ParsedSubcommand> parseSubcommandBindings(BaseCommand instance) {
        List<ParsedSubcommand> subcommands = new ArrayList<>();
        for (Method method : instance.getClass().getDeclaredMethods()) {
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            if (subcommand == null) {
                continue;
            }
            subcommands.add(new ParsedSubcommand(
                    subcommand.parent(),
                    AnnotatedCommandHandlerParser.parseSubcommand(method, subcommand, instance.getClass())
            ));
        }
        return subcommands;
    }

    /**
     * Collects every root alias exposed by the holder.
     *
     * Parses all root aliases from a holder.
     *
     * @param instance command holder instance
     * @return unique set of root aliases
     */
    public Set<String> parseRootAliases(BaseCommand instance) {
        Set<String> aliases = new LinkedHashSet<>();
        for (CommandDescriptor root : this.parseAll(instance)) {
            java.util.Collections.addAll(aliases, root.getNames());
        }
        return aliases;
    }

    /**
     * Adds one root descriptor and indexes all aliases for fast owner resolution.
     *
     * @param roots ordered root descriptor list
     * @param rootsByAlias alias-to-root index
     * @param root root descriptor to add
     */
    private void addRoot(
            List<CommandDescriptor> roots,
            Map<String, CommandDescriptor> rootsByAlias,
            CommandDescriptor root
    ) {
        roots.add(root);
        for (String name : root.getNames()) {
            String key = name.toLowerCase(Locale.ROOT);
            CommandDescriptor previous = rootsByAlias.put(key, root);
            if (previous != null && previous != root) {
                throw new DuplicateCommandException("Duplicate root command alias detected: " + name);
            }
        }
    }

    /**
     * Resolves which root owns a parsed subcommand binding.
     *
     * <p>Resolution order:
     * explicit parent alias, single-root implicit ownership, otherwise mismatch error.
     *
     * @param parent declared parent alias from {@code @Subcommand}
     * @param roots parsed roots in declaration order
     * @param rootsByAlias root lookup index
     * @param holderClass declaring holder class for diagnostics
     * @return resolved owning root descriptor
     */
    private CommandDescriptor resolveOwner(
            String parent,
            List<CommandDescriptor> roots,
            Map<String, CommandDescriptor> rootsByAlias,
            Class<?> holderClass
    ) {
        if (parent != null && !parent.trim().isEmpty()) {
            CommandDescriptor descriptor = rootsByAlias.get(parent.trim().toLowerCase(Locale.ROOT));
            if (descriptor == null) {
                throw new SubcommandParentMismatchException(
                        "Subcommand parent '" + parent + "' was not found in " + holderClass.getName()
                );
            }
            return descriptor;
        }

        if (roots.size() == 1) {
            return roots.get(0);
        }

        throw new SubcommandParentMismatchException(
                "Ambiguous subcommand ownership in " + holderClass.getName()
                        + ". Multiple roots are declared, so use @Subcommand(parent = \"root\")."
        );
    }

    /**
     * Internal pairing of a parsed subcommand descriptor with its declared parent alias.
     */
    private static final class SubcommandBinding {
        private final String parent;
        private final CommandMethodDescriptor descriptor;

        /**
         * @param parent declared parent alias (may be empty)
         * @param descriptor parsed subcommand descriptor
         */
        private SubcommandBinding(String parent, CommandMethodDescriptor descriptor) {
            this.parent = parent;
            this.descriptor = descriptor;
        }
    }

    /**
     * Adds normalized root aliases to the primary-root alias set.
     *
     * @param aliases normalized alias accumulator
     * @param names aliases to normalize and add
     */
    private void addPrimaryAliases(Set<String> aliases, String[] names) {
        for (String name : names) {
            aliases.add(name.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Ensures method-level shortcut root aliases do not shadow existing primary root aliases.
     *
     * @param primaryRootAliases normalized primary root aliases
     * @param shortcutNames shortcut aliases declared on method root definition
     * @param method method declaring shortcut aliases
     * @param holderClass holder class for diagnostics
     */
    private void ensureShortcutAliasDoesNotCollideWithPrimary(
            Set<String> primaryRootAliases,
            String[] shortcutNames,
            Method method,
            Class<?> holderClass
    ) {
        for (String shortcutName : shortcutNames) {
            String normalized = shortcutName.toLowerCase(Locale.ROOT);
            if (primaryRootAliases.contains(normalized)) {
                throw new SubcommandShortcutAliasConflictException(
                        "Shortcut alias '" + shortcutName + "' on method " + method.getName()
                                + " in " + holderClass.getName()
                                + " collides with an existing primary command alias."
                );
            }
        }
    }

    /**
     * Parsed subcommand with optional explicit parent root alias.
     */
    public static final class ParsedSubcommand {
        private final String parent;
        private final CommandMethodDescriptor descriptor;

        /**
         * @param parent declared parent root alias (may be empty)
         * @param descriptor parsed subcommand descriptor
         */
        public ParsedSubcommand(String parent, CommandMethodDescriptor descriptor) {
            this.parent = parent;
            this.descriptor = descriptor;
        }

        /**
         * @return declared parent root alias, or empty when unresolved
         */
        public String getParent() {
            return parent;
        }

        /**
         * @return parsed subcommand descriptor
         */
        public CommandMethodDescriptor getDescriptor() {
            return descriptor;
        }
    }
}
