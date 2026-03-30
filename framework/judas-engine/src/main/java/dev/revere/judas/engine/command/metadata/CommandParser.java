package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Arg;
import dev.revere.judas.api.annotation.RootCommand;
import dev.revere.judas.api.annotation.Shortcut;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.exception.DuplicateCommandException;
import dev.revere.judas.model.exception.SubcommandParentMismatchException;
import dev.revere.judas.model.exception.SubcommandShortcutAliasConflictException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
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
        List<CommandDescriptor> primaryRoots = new ArrayList<>();
        Map<String, CommandDescriptor> rootsByAlias = new LinkedHashMap<>();
        Set<String> primaryRootAliases = new LinkedHashSet<>();
        Method[] declaredMethods = clazz.getDeclaredMethods();

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
            this.addRoot(roots, rootsByAlias, root, clazz);
            primaryRoots.add(root);
            this.addPrimaryAliases(primaryRootAliases, classRootCommand.names());
            this.attachImplicitClassDefaultMethodIfPresent(root, classRootCommand, declaredMethods, clazz);
        }

        for (Method method : declaredMethods) {
            RootCommand methodRootCommand = method.getAnnotation(RootCommand.class);
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            if (methodRootCommand != null && subcommand == null) {
                this.addPrimaryAliases(primaryRootAliases, methodRootCommand.names());
            }
        }

        List<SubcommandBinding> subcommands = new ArrayList<>();

        for (Method method : declaredMethods) {
            Subcommand subcommand = method.getAnnotation(Subcommand.class);
            RootCommand methodRootCommand = method.getAnnotation(RootCommand.class);
            Shortcut shortcut = method.getAnnotation(Shortcut.class);

            if (shortcut != null && methodRootCommand != null) {
                throw new IllegalArgumentException(
                        "Method " + method.getName() + " in " + clazz.getName()
                                + " cannot declare both @RootCommand and @Shortcut."
                );
            }

            if (shortcut != null && subcommand == null) {
                throw new IllegalArgumentException(
                        "Method " + method.getName() + " in " + clazz.getName()
                                + " declares @Shortcut but is missing @Subcommand."
                );
            }

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

                    CommandDescriptor shortcutRoot = this.buildMethodRootDescriptor(
                            method,
                            methodRootCommand.names(),
                            methodRootCommand.hidden(),
                            methodRootCommand.generateHelp(),
                            classPermission,
                            classDescription,
                            classConditions,
                            instance
                    );
                    this.addRoot(roots, rootsByAlias, shortcutRoot, clazz);
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
                this.addRoot(roots, rootsByAlias, methodRoot, clazz);
                primaryRoots.add(methodRoot);
                this.addPrimaryAliases(primaryRootAliases, methodRootCommand.names());
                continue;
            }

            if (subcommand != null && shortcut != null) {
                subcommands.add(new SubcommandBinding(
                        subcommand.parent(),
                        AnnotatedCommandHandlerParser.parseSubcommand(method, subcommand, clazz)
                ));

                this.ensureShortcutAliasDoesNotCollideWithPrimary(
                        primaryRootAliases,
                        shortcut.names(),
                        method,
                        clazz
                );

                CommandDescriptor shortcutRoot = this.buildMethodRootDescriptor(
                        method,
                        shortcut.names(),
                        shortcut.hidden(),
                        false,
                        classPermission,
                        classDescription,
                        classConditions,
                        instance
                );
                this.addRoot(roots, rootsByAlias, shortcutRoot, clazz);
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
            CommandDescriptor owner = this.resolveOwner(binding.parent, roots, rootsByAlias, primaryRoots, clazz);
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
     * @param names root aliases to expose
     * @param hidden root visibility
     * @param generateHelp whether generated help should be exposed for this root
     * @param classPermission class-level fallback permission
     * @param classDescription class-level fallback description
     * @param classConditions class-level root conditions
     * @param instance holder instance backing reflective invocation
     * @return root descriptor whose default method points to {@code method}
     */
    private CommandDescriptor buildMethodRootDescriptor(
            Method method,
            String[] names,
            boolean hidden,
            boolean generateHelp,
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
                names,
                permission,
                description,
                hidden,
                instance,
                generateHelp,
                CommandConditionExpressionNormalizer.normalize(classConditions)
        );
        descriptor.setDefaultMethod(AnnotatedCommandHandlerParser.parseNamedHandler(
                method,
                names,
                hidden,
                instance.getClass()
        ));
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
            CommandDescriptor root,
            Class<?> holderClass
    ) {
        roots.add(root);
        for (String name : root.getNames()) {
            String key = name.toLowerCase(Locale.ROOT);
            CommandDescriptor previous = rootsByAlias.put(key, root);
            if (previous != null && previous != root) {
                throw new DuplicateCommandException(
                        "Duplicate root command alias '" + name + "' while parsing holder " + holderClass.getName()
                                + ". Existing aliases=" + Arrays.toString(previous.getNames())
                                + ", incoming aliases=" + Arrays.toString(root.getNames()) + "."
                );
            }
        }
    }

    private void attachImplicitClassDefaultMethodIfPresent(
            CommandDescriptor classRoot,
            RootCommand classRootCommand,
            Method[] declaredMethods,
            Class<?> holderClass
    ) {
        List<Method> candidates = new ArrayList<>();
        for (Method method : declaredMethods) {
            if (!isImplicitClassDefaultCandidate(method)) {
                continue;
            }
            candidates.add(method);
        }

        if (candidates.isEmpty()) {
            return;
        }

        if (candidates.size() > 1) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                if (i > 0) {
                    names.append(", ");
                }
                names.append(candidates.get(i).getName());
            }
            throw new IllegalArgumentException(
                    "Ambiguous default root handler candidates for class-level root in " + holderClass.getName()
                            + ": " + names + ". Annotate one method with @RootCommand to make intent explicit."
            );
        }

        Method candidate = candidates.get(0);
        classRoot.setDefaultMethod(AnnotatedCommandHandlerParser.parseNamedHandler(
                candidate,
                classRootCommand.names(),
                classRootCommand.hidden(),
                holderClass
        ));
    }

    private static boolean isImplicitClassDefaultCandidate(Method method) {
        if (method.getAnnotation(RootCommand.class) != null
                || method.getAnnotation(Subcommand.class) != null
                || method.getAnnotation(Shortcut.class) != null
                || method.isSynthetic()
                || method.isBridge()
                || Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        if (method.getAnnotation(Permission.class) != null
                || method.getAnnotation(Description.class) != null
                || method.getAnnotation(Conditions.class) != null) {
            return true;
        }

        for (Parameter parameter : method.getParameters()) {
            if (parameter.getAnnotation(Sender.class) != null
                    || parameter.getAnnotation(Arg.class) != null
                    || CommandContext.class.isAssignableFrom(parameter.getType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resolves which root owns a parsed subcommand binding.
     *
     * <p>Resolution order:
     * explicit parent alias, single primary-root implicit ownership, single-root implicit ownership, otherwise mismatch error.
     *
     * @param parent declared parent alias from {@code @Subcommand}
     * @param roots parsed roots in declaration order
     * @param rootsByAlias root lookup index
     * @param primaryRoots parsed class/method primary roots (excludes shortcuts)
     * @param holderClass declaring holder class for diagnostics
     * @return resolved owning root descriptor
     */
    private CommandDescriptor resolveOwner(
            String parent,
            List<CommandDescriptor> roots,
            Map<String, CommandDescriptor> rootsByAlias,
            List<CommandDescriptor> primaryRoots,
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

        if (primaryRoots.size() == 1) {
            return primaryRoots.get(0);
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
