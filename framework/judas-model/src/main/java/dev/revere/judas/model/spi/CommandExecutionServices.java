package dev.revere.judas.model.spi;

import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.condition.ConditionRegistry;
import dev.revere.judas.model.middleware.CommandMiddleware;
import dev.revere.judas.model.resolver.ParameterResolver;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Resolver and suggestion wiring exposed to the engine by the runtime command manager.
 */
public interface CommandExecutionServices {
    ConditionRegistry EMPTY_CONDITION_REGISTRY = new ConditionRegistry();
    Executor DIRECT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

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

    /**
     * @return condition registry used for command execution validation
     */
    default ConditionRegistry getConditionRegistry() {
        return EMPTY_CONDITION_REGISTRY;
    }

    /**
     * @return immutable middleware list executed around command invocation
     */
    default List<CommandMiddleware> getMiddlewares() {
        return Collections.emptyList();
    }

    /**
     * @return cooldown service used for rate limiting command execution
     */
    default CooldownService getCooldownService() {
        return CooldownService.NO_OP;
    }

    /**
     * @return async executor used for {@code @Async} handlers
     */
    default Executor getAsyncExecutor() {
        return DIRECT_EXECUTOR;
    }

    /**
     * @return ordered response handlers for non-void method return values
     */
    default List<CommandResponseHandler> getResponseHandlers() {
        return Collections.emptyList();
    }
}
