package dev.revere.judas.engine.command.metadata;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.RootCommand;
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
     * @param ownerType declaring holder type
     * @return immutable method descriptor
     */
    public static CommandMethodDescriptor parseSubcommand(Method method, Subcommand subcommand, Class<?> ownerType) {
        return parse(method, subcommand.names(), subcommand.hidden(), ownerType);
    }

    /**
     * Parses a default/root handler declaration.
     *
     * @param method reflective method reference
     * @param rootCommand root command annotation metadata
     * @param ownerType declaring holder type
     * @return immutable method descriptor
     */
    public static CommandMethodDescriptor parseDefaultHandler(Method method, RootCommand rootCommand, Class<?> ownerType) {
        return parse(method, rootCommand.names(), rootCommand.hidden(), ownerType);
    }

    /**
     * Parses shared method metadata for both default and subcommand handlers.
     *
     * @param method handler method
     * @param names exposed aliases for this handler
     * @param hidden whether handler should be hidden from generated help output
     * @param ownerType declaring holder type used for annotation inheritance lookups
     * @return immutable command-method descriptor
     */
    private static CommandMethodDescriptor parse(Method method, String[] names, boolean hidden, Class<?> ownerType) {
        Permission permission = method.getAnnotation(Permission.class);
        Description description = method.getAnnotation(Description.class);
        Conditions conditions = method.getAnnotation(Conditions.class);

        return new CommandMethodDescriptor(
                names,
                permission != null ? permission.value() : null,
                description != null ? description.value() : null,
                hidden,
                method,
                MethodParameterIntrospector.describeParameters(method),
                CommandConditionExpressionNormalizer.normalize(conditions),
                CommandMethodExecutionMetadataResolver.resolveAsyncExecution(ownerType, method),
                CommandMethodExecutionMetadataResolver.resolveCooldown(ownerType, method)
        );
    }
}
