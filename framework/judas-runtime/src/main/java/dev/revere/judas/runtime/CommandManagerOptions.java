package dev.revere.judas.runtime;

import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;
import dev.revere.judas.model.spi.CommandResponseHandler;
import dev.revere.judas.model.spi.CooldownService;
import dev.revere.judas.model.spi.JudasLogger;
import dev.revere.judas.model.condition.ConditionRegistry;
import dev.revere.judas.model.middleware.CommandMiddleware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Runtime-level customization options for one manager instance.
 *
 * <p>This builder covers the full runtime surface around the engine: player-facing messages, help rendering,
 * extension registries, middleware, framework logging, cooldown handling, async execution, and response handling.
 */
public final class CommandManagerOptions {
    private final CommandMessageProvider messageProvider;
    private final CommandHelpFormatter helpFormatter;
    private final String helpSubcommandName;
    private final boolean showUsageAfterBindingError;
    private final ConditionRegistry conditionRegistry;
    private final List<CommandMiddleware> middlewares;
    private final JudasLogger logger;
    private final CooldownService cooldownService;
    private final Executor asyncExecutor;
    private final List<CommandResponseHandler> responseHandlers;

    private CommandManagerOptions(Builder builder) {
        this.messageProvider = builder.messageProvider != null ? builder.messageProvider : new DefaultCommandMessageProvider();
        this.helpFormatter = builder.helpFormatter != null ? builder.helpFormatter : new DefaultCommandHelpFormatter();
        this.helpSubcommandName = normalizeHelpSubcommandName(builder.helpSubcommandName);
        this.showUsageAfterBindingError = builder.showUsageAfterBindingError;
        this.conditionRegistry = builder.conditionRegistry != null ? builder.conditionRegistry : new ConditionRegistry();
        this.middlewares = Collections.unmodifiableList(new ArrayList<>(builder.middlewares));
        this.logger = builder.logger != null ? builder.logger : new DefaultJudasLogger();
        this.cooldownService = builder.cooldownService != null ? builder.cooldownService : new InMemoryCooldownService();
        this.asyncExecutor = builder.asyncExecutor != null ? builder.asyncExecutor : new DefaultAsyncExecutor();
        if (builder.responseHandlers.isEmpty()) {
            this.responseHandlers = Collections.<CommandResponseHandler>singletonList(new DefaultCommandResponseHandler());
        } else {
            this.responseHandlers = Collections.unmodifiableList(new ArrayList<>(builder.responseHandlers));
        }
    }

    /**
     * @return configured message provider
     */
    public CommandMessageProvider getMessageProvider() {
        return this.messageProvider;
    }

    /**
     * @return configured help formatter
     */
    public CommandHelpFormatter getHelpFormatter() {
        return this.helpFormatter;
    }

    /**
     * @return configured generated help subcommand name
     */
    public String getHelpSubcommandName() {
        return this.helpSubcommandName;
    }

    /**
     * @return {@code true} when subcommand usage should be appended after binding failures
     */
    public boolean isShowUsageAfterBindingError() {
        return this.showUsageAfterBindingError;
    }

    /**
     * @return configured condition registry
     */
    public ConditionRegistry getConditionRegistry() {
        return this.conditionRegistry;
    }

    /**
     * @return immutable middleware list
     */
    public List<CommandMiddleware> getMiddlewares() {
        return this.middlewares;
    }

    /**
     * @return framework logger used for initialization/debug lifecycle logs
     */
    public JudasLogger getLogger() {
        return this.logger;
    }

    /**
     * @return configured cooldown service
     */
    public CooldownService getCooldownService() {
        return this.cooldownService;
    }

    /**
     * @return async executor used for {@code @Async} handlers
     */
    public Executor getAsyncExecutor() {
        return this.asyncExecutor;
    }

    /**
     * @return immutable response handlers
     */
    public List<CommandResponseHandler> getResponseHandlers() {
        return this.responseHandlers;
    }

    /**
     * @return new mutable builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private static String normalizeHelpSubcommandName(String value) {
        String out = value == null ? "" : value.trim();
        if (out.isEmpty()) {
            return "help";
        }
        return out;
    }

    /**
     * Mutable builder for {@link CommandManagerOptions}.
     */
    public static final class Builder {
        private CommandMessageProvider messageProvider;
        private CommandHelpFormatter helpFormatter;
        private String helpSubcommandName = "help";
        private boolean showUsageAfterBindingError = true;
        private ConditionRegistry conditionRegistry;
        private final List<CommandMiddleware> middlewares = new ArrayList<>();
        private JudasLogger logger;
        private CooldownService cooldownService;
        private Executor asyncExecutor;
        private final List<CommandResponseHandler> responseHandlers = new ArrayList<>();

        /**
         * Sets a custom command-message provider for user-facing framework responses.
         *
         * @param value custom message provider
         * @return this builder
         */
        public Builder messageProvider(CommandMessageProvider value) {
            this.messageProvider = value;
            return this;
        }

        /**
         * Sets a custom help formatter for generated/usage output.
         *
         * @param value custom help formatter
         * @return this builder
         */
        public Builder helpFormatter(CommandHelpFormatter value) {
            this.helpFormatter = value;
            return this;
        }

        /**
         * Sets the generated-help subcommand alias token.
         *
         * @param value generated help subcommand alias
         * @return this builder
         */
        public Builder helpSubcommandName(String value) {
            this.helpSubcommandName = value;
            return this;
        }

        /**
         * Enables or disables automatic usage hints after binding errors.
         *
         * @param value whether to append concise usage after binding failures
         * @return this builder
         */
        public Builder showUsageAfterBindingError(boolean value) {
            this.showUsageAfterBindingError = value;
            return this;
        }

        /**
         * Replaces the condition registry used for expression lookup.
         *
         * @param value condition registry to use
         * @return this builder
         */
        public Builder conditionRegistry(ConditionRegistry value) {
            this.conditionRegistry = value;
            return this;
        }

        /**
         * Appends one middleware to the execution chain.
         *
         * @param middleware middleware to append
         * @return this builder
         */
        public Builder addMiddleware(CommandMiddleware middleware) {
            if (middleware == null) {
                throw new IllegalArgumentException("middleware must not be null");
            }
            this.middlewares.add(middleware);
            return this;
        }

        /**
         * Sets the framework logger implementation.
         *
         * @param value framework logger implementation
         * @return this builder
         */
        public Builder logger(JudasLogger value) {
            this.logger = value;
            return this;
        }

        /**
         * Sets the cooldown service implementation.
         *
         * @param value cooldown service implementation
         * @return this builder
         */
        public Builder cooldownService(CooldownService value) {
            this.cooldownService = value;
            return this;
        }

        /**
         * Sets the async executor used by {@code @Async} handlers.
         *
         * @param value async executor
         * @return this builder
         */
        public Builder asyncExecutor(Executor value) {
            this.asyncExecutor = value;
            return this;
        }

        /**
         * Appends one response handler to the dispatch chain.
         *
         * @param handler response handler to append
         * @return this builder
         */
        public Builder addResponseHandler(CommandResponseHandler handler) {
            if (handler == null) {
                throw new IllegalArgumentException("handler must not be null");
            }
            this.responseHandlers.add(handler);
            return this;
        }

        /**
         * Builds an immutable options object from current builder state.
         *
         * @return immutable options instance
         */
        public CommandManagerOptions build() {
            return new CommandManagerOptions(this);
        }
    }
}
