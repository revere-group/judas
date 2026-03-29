package dev.revere.judas.runtime;

import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.JudasLogger;

/**
 * Warns when explicit suggestions overlap with an already-registered typed resolver.
 */
public final class RedundantSuggestionWarningAnalyzer {
    private final JudasLogger logger;

    /**
     * Creates an analyzer that emits framework-side startup warnings.
     *
     * @param logger framework logger used for warnings
     */
    public RedundantSuggestionWarningAnalyzer(JudasLogger logger) {
        this.logger = logger;
    }

    /**
     * Scans one command descriptor and warns about resolver/suggestion overlap.
     *
     * <p>A warning is emitted when:
     * <ul>
     *     <li>the parameter type is not {@link String},</li>
     *     <li>explicit suggestions are declared (provider or literals), and</li>
     *     <li>a parameter resolver for that same type is already registered.</li>
     * </ul>
     *
     * @param descriptor descriptor being registered
     * @param resolverLookup resolver lookup callback
     */
    public void analyze(CommandDescriptor descriptor, ResolverLookup resolverLookup) {
        this.warnForMethod(descriptor, descriptor.getDefaultMethod(), resolverLookup);
        for (CommandMethodDescriptor subcommand : descriptor.getSubcommands()) {
            this.warnForMethod(descriptor, subcommand, resolverLookup);
        }
    }

    private void warnForMethod(
            CommandDescriptor root,
            CommandMethodDescriptor method,
            ResolverLookup resolverLookup
    ) {
        if (method == null) {
            return;
        }
        for (ParameterDescriptor parameter : method.getParameters()) {
            if (!this.isPotentiallyRedundant(parameter, resolverLookup)) {
                continue;
            }
            this.logger.warn(
                    "Redundant suggestions detected for /" + root.getNames()[0]
                            + " " + method.getNames()[0]
                            + " parameter '" + parameter.getName() + "' (type " + parameter.getType().getSimpleName() + "). "
                            + "A resolver for this type is already registered, so @Suggestions may be unnecessary."
            );
        }
    }

    private boolean isPotentiallyRedundant(ParameterDescriptor parameter, ResolverLookup resolverLookup) {
        if (parameter.getType() == String.class) {
            return false;
        }
        boolean hasExplicitSuggestions =
                parameter.getSuggestionProviderType() != null || parameter.getInlineSuggestions().length > 0;
        if (!hasExplicitSuggestions) {
            return false;
        }
        return resolverLookup.get(parameter.getType()) != null;
    }

    /**
     * Small lookup contract that keeps the analyzer detached from manager implementation details.
     */
    public interface ResolverLookup {
        /**
         * Resolves a registered resolver for one parameter type.
         *
         * @param type parameter type to inspect
         * @param <T> parameter type
         * @return resolver instance, or {@code null} when not registered
         */
        <T> ParameterResolver<T> get(Class<T> type);
    }
}
