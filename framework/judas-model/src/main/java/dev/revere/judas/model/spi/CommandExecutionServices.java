package dev.revere.judas.model.spi;

import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.resolver.ParameterResolver;

/**
 * Resolver and suggestion wiring exposed to the engine by the runtime command manager.
 */
public interface CommandExecutionServices {

    /**
     * @param type parameter type
     * @param <T> parameter type
     * @return registered resolver, or {@code null}
     */
    <T> ParameterResolver<T> getResolver(Class<T> type);

    /**
     * @param type suggestion provider class key
     * @return cached or newly created provider instance
     */
    SuggestionProvider resolveSuggestionProvider(Class<? extends SuggestionProvider> type);

    /**
     * @return framework message provider used for permission, usage and error feedback
     */
    CommandMessageProvider getMessageProvider();

    /**
     * @return formatter used for generated help and usage output
     */
    CommandHelpFormatter getHelpFormatter();

    /**
     * @return generated help subcommand name
     */
    String getHelpSubcommandName();

    /**
     * @return {@code true} when usage hints should be shown after binding errors
     */
    boolean isShowUsageAfterBindingError();
}
