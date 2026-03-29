package dev.revere.judas.runtime;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.engine.command.CommandCompletionService;
import dev.revere.judas.engine.command.CommandDescriptorBuilder;
import dev.revere.judas.engine.command.CommandParser;
import dev.revere.judas.engine.command.CommandRouter;
import dev.revere.judas.engine.command.SubcommandRegistrationCoordinator;
import dev.revere.judas.engine.resolver.BuiltinParameterResolvers;
import dev.revere.judas.engine.resolver.ParameterResolverRegistry;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.exception.DuplicateCommandException;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandExecutionServices;
import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages command registration and {@link ParameterResolver} wiring for a runtime.
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
     * Registers a command using {@link dev.revere.judas.api.annotation.Definition} metadata for names.
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
     * Manually links a parsed subcommand descriptor under an already-registered root command.
     *
     * @param rootName root command alias to attach under
     * @param subcommand parsed subcommand descriptor
     */
    public void registerSubcommand(String rootName, CommandMethodDescriptor subcommand) {
        this.subcommandCoordinator.registerSubcommand(rootName, subcommand);
    }

    /**
     * Manually links one subcommand from a holder by name under an already-registered root command.
     *
     * @param rootName root command alias to attach under
     * @param holder source holder containing subcommand methods
     * @param subcommandAlias subcommand alias to attach
     */
    public void registerSubcommand(String rootName, BaseCommand holder, String subcommandAlias) {
        this.registerSubcommands(rootName, holder, subcommandAlias);
    }

    /**
     * Registers all subcommands from a holder under a specific root.
     *
     * @param rootName root command alias to attach under
     * @param holder source holder containing subcommand methods
     */
    public void registerSubcommands(String rootName, BaseCommand holder) {
        this.subcommandCoordinator.registerSubcommands(rootName, holder);
    }

    /**
     * Registers selected subcommands from a holder under a specific root.
     *
     * @param rootName root command alias to attach under
     * @param holder source holder containing subcommand methods
     * @param subcommandAliases selected aliases to attach
     */
    public void registerSubcommands(String rootName, BaseCommand holder, String... subcommandAliases) {
        this.subcommandCoordinator.registerSubcommands(rootName, holder, subcommandAliases);
    }

    /**
     * Registers all subcommands from a holder by resolving each root from {@code @Subcommand(parent = "...")}.
     *
     * @param holder source holder containing subcommand methods
     */
    public void registerSubcommands(BaseCommand holder) {
        this.subcommandCoordinator.registerSubcommands(holder);
    }

    /**
     * Registers selected subcommands from a holder by resolving root from {@code @Subcommand(parent = "...")}.
     *
     * @param holder source holder containing subcommand methods
     * @param subcommandAliases selected aliases to attach
     */
    public void registerSubcommands(BaseCommand holder, String... subcommandAliases) {
        this.subcommandCoordinator.registerSubcommands(holder, subcommandAliases);
    }

    /**
     * Convenience singular form for selected subcommand + auto root resolution.
     *
     * @param holder source holder containing subcommand methods
     * @param subcommandAlias selected alias to attach
     */
    public void registerSubcommand(BaseCommand holder, String subcommandAlias) {
        this.registerSubcommands(holder, subcommandAlias);
    }

    /**
     * Convenience singular form for registering all subcommands + auto root resolution.
     *
     * @param holder source holder containing subcommand methods
     */
    public void registerSubcommand(BaseCommand holder) {
        this.registerSubcommands(holder);
    }

    /**
     * Registers a prepared descriptor with the manager and the underlying platform.
     *
     * @param descriptor prepared root command descriptor
     */
    public void register(CommandDescriptor descriptor) {
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
     * Exposes registered root commands keyed by alias.
     *
     * @return immutable alias-to-descriptor map
     */
    public Map<String, CommandDescriptor> getCommands() {
        return Collections.unmodifiableMap(this.commands);
    }
}
