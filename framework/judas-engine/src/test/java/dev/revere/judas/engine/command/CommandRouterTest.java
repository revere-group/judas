package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.engine.resolver.BuiltinParameterResolvers;
import dev.revere.judas.engine.resolver.ParameterResolverRegistry;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.resolver.ArgumentTokenReader;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;
import dev.revere.judas.model.spi.CommandExecutionServices;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static class TestServices implements CommandExecutionServices {
        private final ParameterResolverRegistry registry = new ParameterResolverRegistry();
        private final ConcurrentHashMap<Class<? extends SuggestionProvider>, SuggestionProvider> suggestionProviders =
                new ConcurrentHashMap<>();

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
                public String noPermissionForRoot(CommandDescriptor descriptor) {
                    return "You do not have permission to use this command.";
                }

                @Override
                public String noPermissionForSubcommand(CommandDescriptor descriptor, dev.revere.judas.model.command.CommandMethodDescriptor methodDescriptor) {
                    return "You do not have permission to use this subcommand.";
                }

                @Override
                public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
                    return "Unknown subcommand '" + token + "'.";
                }

                @Override
                public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
                    return "This command is missing a handler.";
                }

                @Override
                public String bindingError(
                        CommandDescriptor descriptor,
                        dev.revere.judas.model.command.CommandMethodDescriptor methodDescriptor,
                        dev.revere.judas.model.command.CommandBindingException exception
                ) {
                    return exception.getMessage();
                }

                @Override
                public String executionError(
                        CommandDescriptor descriptor,
                        dev.revere.judas.model.command.CommandMethodDescriptor methodDescriptor,
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
                        dev.revere.judas.model.command.CommandMethodDescriptor methodDescriptor
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
