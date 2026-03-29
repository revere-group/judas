package dev.revere.judas.runtime;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.engine.command.metadata.CommandDescriptorBuilder;
import dev.revere.judas.engine.command.metadata.CommandParser;
import dev.revere.judas.engine.command.metadata.SubcommandRegistrationCoordinator;
import dev.revere.judas.engine.command.completion.CommandCompletionService;
import dev.revere.judas.engine.command.routing.CommandRouter;
import dev.revere.judas.engine.resolver.BuiltinParameterResolvers;
import dev.revere.judas.engine.resolver.ParameterResolverRegistry;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandCondition;
import dev.revere.judas.model.condition.ConditionRegistry;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.exception.DuplicateCommandException;
import dev.revere.judas.model.middleware.CommandMiddleware;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandExecutionServices;
import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;
import dev.revere.judas.model.spi.CommandResponseHandler;
import dev.revere.judas.model.spi.CooldownService;
import dev.revere.judas.model.spi.JudasLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Runtime facade that owns registration, completion, dispatch, and extension wiring for one platform adapter.
 *
 * <p>It exposes the engine-facing {@link CommandExecutionServices} contract while keeping platform managers focused on
 * publishing root descriptors into their own command system.
 */
public abstract class CommandManager implements CommandExecutionServices {
    private final ParameterResolverRegistry resolverRegistry = new ParameterResolverRegistry();
    private final Map<String, CommandDescriptor> commands = new HashMap<>();
    private final Map<Class<? extends SuggestionProvider>, SuggestionProvider> suggestionProviders = new ConcurrentHashMap<>();

    private final CommandParser parser = new CommandParser();
    private final SubcommandRegistrationCoordinator subcommandCoordinator =
            new SubcommandRegistrationCoordinator(this.commands, this.parser);
    private final CommandRouter router = new CommandRouter(this);
    private final CommandCompletionService completionService = new CommandCompletionService(this);
    private final CommandManagerOptions options;
    private final ConditionRegistry conditionRegistry;
    private final List<CommandMiddleware> middlewares;
    private final JudasLogger logger;
    private final CooldownService cooldownService;
    private final Executor asyncExecutor;
    private final List<CommandResponseHandler> responseHandlers;
    private final RedundantSuggestionWarningAnalyzer redundantSuggestionWarningAnalyzer;

    protected CommandManager() {
        this(CommandManagerOptions.builder().build());
    }

    /**
     * @param options runtime customization options
     */
    protected CommandManager(CommandManagerOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        this.options = options;
        this.conditionRegistry = options.getConditionRegistry();
        this.middlewares = new ArrayList<>(options.getMiddlewares());
        this.logger = options.getLogger();
        this.cooldownService = options.getCooldownService();
        this.asyncExecutor = options.getAsyncExecutor();
        this.responseHandlers = new ArrayList<>(options.getResponseHandlers());
        this.redundantSuggestionWarningAnalyzer = new RedundantSuggestionWarningAnalyzer(this.logger);
        BuiltinCommandConditions.registerAll(this.conditionRegistry);
        BuiltinParameterResolvers.registerAll(this.resolverRegistry);
    }

    /**
     * Dispatches execution for a registered root descriptor using the given context.
     *
     * @param descriptor root descriptor mapped by the platform
     * @param context execution context
     */
    public void dispatch(CommandDescriptor descriptor, CommandContext context) {
        this.router.dispatch(descriptor, context);
    }

    /**
     * Computes tab-completion candidates for the given root descriptor and argument tail.
     *
     * @param descriptor registered root descriptor
     * @param adapter platform completion adapter
     * @param args tokens after the command label
     * @return candidate list (never {@code null})
     */
    public List<String> complete(CommandDescriptor descriptor, CompletionAdapter adapter, String[] args) {
        return this.completionService.complete(descriptor, adapter, args);
    }

    /**
     * Overrides the default reflective instance for a {@link dev.revere.judas.api.annotation.Suggestions} type.
     *
     * @param type suggestion provider class key
     * @param instance provider instance to use for that key
     */
    public void registerSuggestionProvider(Class<? extends SuggestionProvider> type, SuggestionProvider instance) {
        if (type == null || instance == null) {
            throw new IllegalArgumentException("type and instance must be non-null");
        }
        this.suggestionProviders.put(type, instance);
    }

    @Override
    public SuggestionProvider resolveSuggestionProvider(Class<? extends SuggestionProvider> type) {
        return this.suggestionProviders.computeIfAbsent(type, t -> {
            try {
                return t.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Suggestion provider must declare a public no-arg constructor: " + t.getName(),
                        exception
                );
            }
        });
    }

    /**
     * @return immutable manager options used for this manager instance
     */
    public CommandManagerOptions getOptions() {
        return this.options;
    }

    /**
     * Registers a command using {@link dev.revere.judas.api.annotation.RootCommand} metadata for names.
     *
     * @param command command holder/root definition object
     */
    public void register(BaseCommand command) {
        for (CommandDescriptor descriptor : this.parser.parseAll(command)) {
            this.register(descriptor);
        }
    }

    /**
     * Registers a command while overriding the exposed names and aliases.
     *
     * @param command command holder/root definition object
     * @param names explicit root aliases to expose
     */
    public void register(BaseCommand command, String... names) {
        if (names == null || names.length == 0) {
            throw new IllegalArgumentException("At least one command name is required.");
        }
        CommandDescriptor parsed = this.parser.parse(command);
        this.register(CommandDescriptorBuilder.withNames(parsed, names));
    }

    /**
     * Attaches a parsed subcommand descriptor under an already registered root.
     *
     * @param rootName   root command alias to attach under
     * @param subcommand parsed subcommand descriptor
     */
    public void registerSub(String rootName, CommandMethodDescriptor subcommand) {
        this.subcommandCoordinator.registerSub(rootName, subcommand);
    }

    /**
     * Registers every compatible subcommand from a holder under one explicit root.
     *
     * @param rootName root command alias to attach under
     * @param holder   source holder containing subcommand methods
     */
    public void registerSub(String rootName, BaseCommand holder) {
        this.subcommandCoordinator.registerSub(rootName, holder);
    }

    /**
     * Registers only handlers whose {@code @Subcommand} aliases intersect {@code subcommandAliases},
     * all under the given root (each method's {@code parent} must be compatible with {@code rootName}).
     *
     * @param rootName            root command alias to attach under
     * @param holder              source holder containing subcommand methods
     * @param subcommandAliases   one or more aliases to match from {@code @Subcommand(names = ...)}
     */
    public void registerSub(String rootName, BaseCommand holder, String... subcommandAliases) {
        this.subcommandCoordinator.registerSub(rootName, holder, subcommandAliases);
    }

    /**
     * Registers all subcommands from a holder using each method's {@code @Subcommand(parent = "...")} to pick the root.
     *
     * @param holder source holder containing subcommand methods
     */
    public void registerSub(BaseCommand holder) {
        this.subcommandCoordinator.registerSub(holder);
    }

    /**
     * Registers only handlers whose subcommand aliases appear in {@code subcommandAliases}, resolving
     * the root per method via {@code @Subcommand(parent = "...")}.
     *
     * @param holder              source holder containing subcommand methods
     * @param subcommandAliases   one or more aliases to match (same overload handles one or many)
     */
    public void registerSub(BaseCommand holder, String... subcommandAliases) {
        this.subcommandCoordinator.registerSub(holder, subcommandAliases);
    }

    /**
     * Registers a prepared descriptor with the manager and the underlying platform.
     *
     * @param descriptor prepared root command descriptor
     */
    public void register(CommandDescriptor descriptor) {
        this.redundantSuggestionWarningAnalyzer.analyze(descriptor, new RedundantSuggestionWarningAnalyzer.ResolverLookup() {
            @Override
            public <T> ParameterResolver<T> get(Class<T> type) {
                return CommandManager.this.getResolver(type);
            }
        });
        for (String name : descriptor.getNames()) {
            String key = name.toLowerCase(Locale.ROOT);
            CommandDescriptor previous = this.commands.put(key, descriptor);
            if (previous != null && previous != descriptor) {
                throw new DuplicateCommandException("Command name '" + name + "' is already registered.");
            }
        }
        this.registerPlatform(descriptor);
    }

    /**
     * Runtime-specific bridge that publishes a root descriptor to the underlying command system.
     *
     * @param descriptor root descriptor to publish
     */
    protected abstract void registerPlatform(CommandDescriptor descriptor);

    /**
     * Registers a resolver for a specific parameter type.
     *
     * @param type target parameter type
     * @param resolver resolver implementation
     * @param <T> parameter type
     */
    public <T> void registerResolver(Class<T> type, ParameterResolver<T> resolver) {
        this.resolverRegistry.register(type, resolver);
        this.logger.debug("Registered parameter resolver for type " + type.getName() + ".");
    }

    /**
     * Registers a named condition implementation.
     *
     * @param key condition key used by {@code @Conditions}
     * @param condition condition implementation
     */
    public void registerCondition(String key, CommandCondition condition) {
        this.conditionRegistry.register(key, condition);
        this.logger.debug("Registered condition '" + key + "'.");
    }

    /**
     * Registers one middleware in execution order.
     *
     * @param middleware middleware to add
     */
    public void registerMiddleware(CommandMiddleware middleware) {
        if (middleware == null) {
            throw new IllegalArgumentException("middleware must not be null");
        }
        this.middlewares.add(middleware);
        this.logger.debug("Registered middleware " + middleware.getClass().getName() + ".");
    }

    @Override
    public <T> ParameterResolver<T> getResolver(Class<T> type) {
        return this.resolverRegistry.get(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandMessageProvider getMessageProvider() {
        return this.options.getMessageProvider();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandHelpFormatter getHelpFormatter() {
        return this.options.getHelpFormatter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpSubcommandName() {
        return this.options.getHelpSubcommandName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShowUsageAfterBindingError() {
        return this.options.isShowUsageAfterBindingError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionRegistry getConditionRegistry() {
        return this.conditionRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CommandMiddleware> getMiddlewares() {
        return Collections.unmodifiableList(this.middlewares);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CooldownService getCooldownService() {
        return this.cooldownService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Executor getAsyncExecutor() {
        return this.asyncExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CommandResponseHandler> getResponseHandlers() {
        return Collections.unmodifiableList(this.responseHandlers);
    }

    /**
     * Exposes registered root commands keyed by alias.
     *
     * @return immutable alias-to-descriptor map
     */
    public Map<String, CommandDescriptor> getCommands() {
        return Collections.unmodifiableMap(this.commands);
    }

    /**
     * @return internal framework logger
     */
    protected JudasLogger getLogger() {
        return this.logger;
    }
}
