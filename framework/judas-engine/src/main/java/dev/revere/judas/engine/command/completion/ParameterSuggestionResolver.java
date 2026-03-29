package dev.revere.judas.engine.command.completion;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandExecutionServices;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolves raw completion candidates for one parameter.
 *
 * <p>Resolution order is deterministic:
 * inline literals first, explicit {@link SuggestionProvider} second, type {@link ParameterResolver} completion last.
 */
public final class ParameterSuggestionResolver {
    private final CommandExecutionServices executionServices;

    /**
     * Creates a parameter suggestion resolver.
     *
     * @param executionServices runtime services used to resolve providers and resolvers
     */
    public ParameterSuggestionResolver(CommandExecutionServices executionServices) {
        this.executionServices = executionServices;
    }

    /**
     * Resolves candidate values for one completion target parameter.
     *
     * <p>This method intentionally returns raw candidates. Prefix filtering is applied by the outer completion service
     * so filtering strategy remains centralized.
     *
     * @param root root descriptor
     * @param handler current handler
     * @param parameter parameter being completed
     * @param adapter platform completion adapter
     * @param partial current partial token
     * @param tokensForHandler full handler token array
     * @return raw candidate list before prefix filtering
     */
    public List<String> resolve(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            ParameterDescriptor parameter,
            CompletionAdapter adapter,
            String partial,
            String[] tokensForHandler
    ) {
        if (parameter.getInlineSuggestions().length > 0) {
            return Arrays.asList(parameter.getInlineSuggestions());
        }

        if (parameter.getSuggestionProviderType() != null) {
            SuggestionProvider provider = this.executionServices.resolveSuggestionProvider(parameter.getSuggestionProviderType());
            return provider.suggest(this.newCompletionContext(root, handler, parameter, adapter, partial, tokensForHandler));
        }

        ParameterResolver<?> resolver = this.executionServices.getResolver(parameter.getType());
        if (resolver == null) {
            return Collections.emptyList();
        }
        return resolver.complete(this.newCompletionContext(root, handler, parameter, adapter, partial, tokensForHandler));
    }

    /**
     * Builds completion context forwarded to providers and resolvers.
     *
     * @param root root descriptor
     * @param handler current handler
     * @param parameter target parameter
     * @param adapter platform completion adapter
     * @param partial current partial token
     * @param tokensForHandler full handler token array
     * @return completion context
     */
    private CompletionContext newCompletionContext(
            CommandDescriptor root,
            CommandMethodDescriptor handler,
            ParameterDescriptor parameter,
            CompletionAdapter adapter,
            String partial,
            String[] tokensForHandler
    ) {
        return new CompletionContext(
                adapter.getSender(),
                root,
                handler,
                parameter,
                partial,
                tokensForHandler
        );
    }
}
