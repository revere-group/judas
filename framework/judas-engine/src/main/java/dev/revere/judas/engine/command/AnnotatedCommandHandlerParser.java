package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.CommandMethodDescriptor;

import java.lang.reflect.Method;

/**
 * Converts annotation-tagged methods into immutable command handler descriptors.
 *
 * <p>Parser responsibilities here are intentionally narrow: this class only maps method-level metadata and
 * delegates parameter details to {@link MethodParameterIntrospector}.
 */
public final class AnnotatedCommandHandlerParser {

    private AnnotatedCommandHandlerParser() {
    }

    /**
     * Parses a subcommand method declaration.
     *
     * @param method reflective method reference
     * @param subcommand subcommand annotation metadata
     * @return immutable method descriptor
     */
    public static CommandMethodDescriptor parseSubcommand(Method method, Subcommand subcommand) {
        return parse(method, subcommand.names(), subcommand.hidden());
    }

    /**
     * Parses a default/root handler declaration.
     *
     * @param method reflective method reference
     * @param definition definition annotation metadata
     * @return immutable method descriptor
     */
    public static CommandMethodDescriptor parseDefaultHandler(Method method, Definition definition) {
        return parse(method, definition.names(), definition.hidden());
    }

    private static CommandMethodDescriptor parse(Method method, String[] names, boolean hidden) {
        Permission permission = method.getAnnotation(Permission.class);
        Description description = method.getAnnotation(Description.class);

        return new CommandMethodDescriptor(
                names,
                permission != null ? permission.value() : null,
                description != null ? description.value() : null,
                hidden,
                method,
                MethodParameterIntrospector.describeParameters(method)
        );
    }
}
