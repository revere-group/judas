package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Cooldown;
import dev.revere.judas.api.annotation.CooldownScope;
import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Async;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.engine.command.metadata.CommandParser;
import dev.revere.judas.engine.command.routing.CommandRouter;
import dev.revere.judas.engine.resolver.BuiltinParameterResolvers;
import dev.revere.judas.engine.resolver.ParameterResolverRegistry;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.condition.CommandCondition;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.condition.ConditionContext;
import dev.revere.judas.model.condition.ConditionRegistry;
import dev.revere.judas.model.middleware.CommandExecutionChain;
import dev.revere.judas.model.middleware.CommandExecutionContext;
import dev.revere.judas.model.middleware.CommandMiddleware;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;
import dev.revere.judas.model.spi.CommandExecutionServices;
import dev.revere.judas.model.spi.CommandResponseHandler;
import dev.revere.judas.model.spi.CooldownService;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandRouterTest {

    @Test
    public void rejectsCommandWhenRootPermissionIsMissing() {
        TestServices services = new TestServices();
        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"set", "Alpha", "SkinX"});
        router.dispatch(descriptor, context);

        assertFalse(command.invoked);
        assertTrue(context.messages.get(0).contains("do not have permission"));
    }

    @Test
    public void bindsArrayAndOptionalDefaultArguments() {
        TestServices services = new TestServices();
        services.registerResolver(String[].class, context -> {
            String input = ArgumentTokenReader.requireNext(context);
            return input.split(",");
        });
        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"set", "Alpha,Beta", "KnightSkin"});
        context.permissions.add("judas.use");
        context.permissions.add("judas.use.set");

        router.dispatch(descriptor, context);

        assertTrue(command.invoked);
        assertArrayEquals(new String[]{"Alpha", "Beta"}, command.targets);
        assertEquals("KnightSkin", command.skin);
        assertEquals("Default", command.rank);
        assertFalse(command.silent);
    }

    @Test
    public void bindsOutOfOrderNamedOptionsAndFlags() {
        TestServices services = new TestServices();
        services.registerResolver(String[].class, context -> {
            String input = ArgumentTokenReader.requireNext(context);
            return input.split(",");
        });
        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(
                new TestSender("Revere"),
                new String[]{"set", "Alpha,Beta", "--rank", "Legend", "-s", "--skin", "KnightSkin"}
        );
        context.permissions.add("judas.use");
        context.permissions.add("judas.use.set");

        router.dispatch(descriptor, context);

        assertTrue(command.invoked);
        assertEquals("KnightSkin", command.skin);
        assertEquals("Legend", command.rank);
        assertTrue(command.silent);
    }

    @Test
    public void reportsMissingArguments() {
        TestServices services = new TestServices();
        services.registerResolver(String[].class, context -> {
            String input = ArgumentTokenReader.requireNext(context);
            return input.split(",");
        });
        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"set", "Alpha"});
        context.permissions.add("judas.use");
        context.permissions.add("judas.use.set");

        router.dispatch(descriptor, context);

        assertFalse(command.invoked);
        assertTrue(context.messages.get(0).contains("Missing argument: skin"));
    }

    @Test
    public void consumesRemainingArgumentsWhenConfigured() {
        TestServices services = new TestServices();
        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"broadcast", "hello", "to", "everyone"});
        context.permissions.add("judas.use");

        router.dispatch(descriptor, context);

        assertEquals("hello to everyone", command.broadcastMessage);
    }

    @Test
    public void dispatchesGeneratedHelpSubcommandWhenEnabled() {
        TestServices services = new TestServices();
        CommandRouter router = new CommandRouter(services);
        HelpEnabledCommand command = new HelpEnabledCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"help"});
        router.dispatch(descriptor, context);

        assertTrue(context.messages.stream().anyMatch(line -> line.contains("generated help")));
    }

    @Test
    public void blocksExecutionWhenMethodConditionFails() {
        TestServices services = new TestServices();
        services.getMutableConditionRegistry().register("blocked", new CommandCondition() {
            @Override
            public void validate(ConditionContext context) {
                throw new CommandConditionException("blocked by condition");
            }
        });

        CommandRouter router = new CommandRouter(services);
        ConditionedCommand command = new ConditionedCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);
        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"run"});
        router.dispatch(descriptor, context);

        assertFalse(command.invoked);
        assertTrue(context.messages.contains("blocked by condition"));
    }

    @Test
    public void runsMiddlewareInExpectedOrder() {
        TestServices services = new TestServices();
        services.addMiddleware(new CommandMiddleware() {
            @Override
            public void handle(CommandExecutionContext context, CommandExecutionChain chain) {
                services.order.add("before");
                chain.proceed();
                services.order.add("after");
            }
        });

        CommandRouter router = new CommandRouter(services);
        ExecutionCommand command = new ExecutionCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);
        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"set", "Alpha", "SkinX"});
        context.permissions.add("judas.use");
        context.permissions.add("judas.use.set");
        router.dispatch(descriptor, context);

        assertEquals(2, services.order.size());
        assertEquals("before", services.order.get(0));
        assertEquals("after", services.order.get(1));
    }

    @Test
    public void blocksExecutionWhenCooldownIsActive() {
        TestServices services = new TestServices();
        CommandRouter router = new CommandRouter(services);
        CooldownCommand command = new CooldownCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext first = new TestContext(new TestSender("Revere"), new String[]{"run"});
        router.dispatch(descriptor, first);
        TestContext second = new TestContext(new TestSender("Revere"), new String[]{"run"});
        router.dispatch(descriptor, second);

        assertTrue(command.invokedCount >= 1);
        assertTrue(second.messages.contains("Cooldown active"));
    }

    @Test
    public void handlesStringReturnValueThroughResponseHandler() {
        TestServices services = new TestServices();
        CommandRouter router = new CommandRouter(services);
        ResponseCommand command = new ResponseCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"ping"});
        router.dispatch(descriptor, context);

        assertTrue(context.messages.contains("pong"));
    }

    @Test
    public void executesAsyncCommandUsingAsyncExecutor() {
        TestServices services = new TestServices();
        services.asyncExecutorInvoked = false;
        CommandRouter router = new CommandRouter(services);
        AsyncCommand command = new AsyncCommand();
        CommandDescriptor descriptor = new CommandParser().parse(command);

        TestContext context = new TestContext(new TestSender("Revere"), new String[]{"run"});
        router.dispatch(descriptor, context);

        assertTrue(services.asyncExecutorInvoked);
        assertTrue(command.invoked);
    }

    @Definition(names = {"disguise"})
    @Permission("judas.use")
    private static class ExecutionCommand extends BaseCommand {
        private boolean invoked;
        private String[] targets;
        private String skin;
        private String rank;
        private boolean silent;
        private String broadcastMessage;

        @Subcommand(names = {"set"})
        @Permission("judas.use.set")
        public void onSet(
                @Sender TestSender sender,
                @Name("targets") String[] targets,
                @Name("skin") @Option(names = {"--skin"}) String skin,
                @Name("rank") @Option(names = {"--rank"}) @Optional @Default("Default") String rank,
                @Name("silent") @Flag(names = {"-s", "--silent"}) boolean silent
        ) {
            this.invoked = true;
            this.targets = targets;
            this.skin = skin;
            this.rank = rank;
            this.silent = silent;
        }

        @Subcommand(names = {"broadcast"})
        public void onBroadcast(@Name("message") @ConsumeRemaining String message) {
            this.broadcastMessage = message;
        }
    }

    @Definition(names = {"arena"}, generateHelp = true)
    private static class HelpEnabledCommand extends BaseCommand {
        @Subcommand(names = {"view"})
        public void view(@Name("id") String id) {
        }
    }

    @Definition(names = {"check"})
    private static class ConditionedCommand extends BaseCommand {
        private boolean invoked;

        @Subcommand(names = {"run"})
        @Conditions({"blocked"})
        public void run() {
            this.invoked = true;
        }
    }

    @Definition(names = {"slow"})
    private static class CooldownCommand extends BaseCommand {
        private int invokedCount;

        @Subcommand(names = {"run"})
        @Cooldown(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS, scope = CooldownScope.SENDER)
        public void run() {
            this.invokedCount++;
        }
    }

    @Definition(names = {"response"})
    private static class ResponseCommand extends BaseCommand {
        @Subcommand(names = {"ping"})
        public String ping() {
            return "pong";
        }
    }

    @Definition(names = {"async"})
    private static class AsyncCommand extends BaseCommand {
        private boolean invoked;

        @Subcommand(names = {"run"})
        @Async
        public void run() {
            this.invoked = true;
        }
    }

    private static class TestServices implements CommandExecutionServices {
        private final ParameterResolverRegistry registry = new ParameterResolverRegistry();
        private final ConcurrentHashMap<Class<? extends SuggestionProvider>, SuggestionProvider> suggestionProviders =
                new ConcurrentHashMap<>();
        private final ConditionRegistry conditionRegistry = new ConditionRegistry();
        private final List<CommandMiddleware> middlewares = new ArrayList<>();
        private final List<String> order = new ArrayList<>();
        private final Map<String, Long> cooldownByKey = new HashMap<>();
        private boolean asyncExecutorInvoked;

        TestServices() {
            BuiltinParameterResolvers.registerAll(this.registry);
        }

        <T> void registerResolver(Class<T> type, ParameterResolver<T> resolver) {
            this.registry.register(type, resolver);
        }

        @Override
        public <T> ParameterResolver<T> getResolver(Class<T> type) {
            return this.registry.get(type);
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

        @Override
        public CommandMessageProvider getMessageProvider() {
            return new CommandMessageProvider() {
                @Override
                public String unknownRootCommand(String rootToken) {
                    return "Unknown command '" + rootToken + "'.";
                }

                @Override
                public String noPermissionForRoot(CommandDescriptor descriptor) {
                    return "You do not have permission to use this command.";
                }

                @Override
                public String noPermissionForSubcommand(CommandDescriptor descriptor, CommandMethodDescriptor methodDescriptor) {
                    return "You do not have permission to use this subcommand.";
                }

                @Override
                public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
                    return "Unknown subcommand '" + token + "'.";
                }

                @Override
                public String cooldownActive(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        long remainingMillis
                ) {
                    return "Cooldown active";
                }

                @Override
                public String conditionError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        CommandConditionException exception
                ) {
                    return exception.getMessage();
                }

                @Override
                public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
                    return "This command is missing a handler.";
                }

                @Override
                public String bindingError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        CommandBindingException exception
                ) {
                    return exception.getMessage();
                }

                @Override
                public String executionError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        RuntimeException exception
                ) {
                    return "An error occurred while executing the command.";
                }
            };
        }

        @Override
        public CommandHelpFormatter getHelpFormatter() {
            return new CommandHelpFormatter() {
                @Override
                public List<String> renderGeneratedHelp(CommandDescriptor descriptor) {
                    List<String> out = new ArrayList<>();
                    out.add("generated help for /" + descriptor.getNames()[0]);
                    return out;
                }

                @Override
                public String renderRootUsage(CommandDescriptor descriptor, String availableSubcommands) {
                    return "Usage: /" + descriptor.getNames()[0];
                }

                @Override
                public String renderSubcommandUsage(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor
                ) {
                    return "Usage: /" + descriptor.getNames()[0] + " " + methodDescriptor.getNames()[0];
                }
            };
        }

        @Override
        public String getHelpSubcommandName() {
            return "help";
        }

        @Override
        public boolean isShowUsageAfterBindingError() {
            return true;
        }

        @Override
        public ConditionRegistry getConditionRegistry() {
            return this.conditionRegistry;
        }

        @Override
        public List<CommandMiddleware> getMiddlewares() {
            return this.middlewares;
        }

        @Override
        public CooldownService getCooldownService() {
            return new CooldownService() {
                @Override
                public long acquireOrGetRemaining(String key, long cooldownMillis) {
                    long now = System.currentTimeMillis();
                    Long expiry = cooldownByKey.get(key);
                    if (expiry != null && expiry > now) {
                        return expiry - now;
                    }
                    cooldownByKey.put(key, now + cooldownMillis);
                    return 0L;
                }
            };
        }

        @Override
        public Executor getAsyncExecutor() {
            return new Executor() {
                @Override
                public void execute(Runnable command) {
                    asyncExecutorInvoked = true;
                    command.run();
                }
            };
        }

        @Override
        public List<CommandResponseHandler> getResponseHandlers() {
            List<CommandResponseHandler> handlers = new ArrayList<>();
            handlers.add(new CommandResponseHandler() {
                @Override
                public boolean supports(Object response) {
                    return response instanceof String;
                }

                @Override
                public void handle(CommandContext context, CommandDescriptor root, CommandMethodDescriptor method, Object response) {
                    context.sendMessage((String) response);
                }
            });
            return handlers;
        }

        ConditionRegistry getMutableConditionRegistry() {
            return this.conditionRegistry;
        }

        void addMiddleware(CommandMiddleware middleware) {
            this.middlewares.add(middleware);
        }
    }

    private static class TestContext implements CommandContext {
        private final TestSender sender;
        private final String[] arguments;
        private final List<String> messages = new ArrayList<>();
        private final Set<String> permissions = new HashSet<>();

        private TestContext(TestSender sender, String[] arguments) {
            this.sender = sender;
            this.arguments = arguments;
        }

        @Override
        public String[] getArguments() {
            return arguments;
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        @Override
        public void sendMessage(CommandColor color, String message) {
            messages.add(message);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }

        @Override
        public String getSenderName() {
            return sender.name;
        }

        @Override
        public Object getSender() {
            return sender;
        }
    }

    private static class TestSender {
        private final String name;

        private TestSender(String name) {
            this.name = name;
        }
    }
}
