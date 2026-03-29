package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.engine.command.completion.CommandCompletionService;
import dev.revere.judas.engine.command.metadata.CommandParser;
import dev.revere.judas.engine.resolver.BuiltinParameterResolvers;
import dev.revere.judas.engine.resolver.ParameterResolverRegistry;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandBindingException;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.completion.CompletionContext;
import dev.revere.judas.model.completion.SuggestionProvider;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.resolver.ParameterResolver;
import dev.revere.judas.model.spi.CommandHelpFormatter;
import dev.revere.judas.model.spi.CommandMessageProvider;
import dev.revere.judas.model.spi.CommandExecutionServices;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandCompletionServiceTest {

    @Test
    public void completesSubcommandNames() {
        TestServices services = new TestServices();
        CommandCompletionService completion = new CommandCompletionService(services);
        CommandDescriptor descriptor = new CommandParser().parse(new SubOnlyCommand());

        List<String> out = completion.complete(descriptor, allowAll(), new String[]{});

        assertTrue(out.contains("view"));
        assertTrue(out.contains("edit"));
    }

    @Test
    public void completesSubcommandByPrefix() {
        TestServices services = new TestServices();
        CommandCompletionService completion = new CommandCompletionService(services);
        CommandDescriptor descriptor = new CommandParser().parse(new SubOnlyCommand());

        List<String> out = completion.complete(descriptor, allowAll(), new String[]{"vi"});

        assertEquals(Arrays.asList("view"), out);
    }

    @Test
    public void completesSuggestionProviderParameter() {
        TestServices services = new TestServices();
        CommandCompletionService completion = new CommandCompletionService(services);
        CommandDescriptor descriptor = new CommandParser().parse(new ArenaCommand());

        List<String> out = completion.complete(descriptor, allowAll(), new String[]{"view", "du"});

        assertEquals(Arrays.asList("duel"), out);
    }

    @Test
    public void completesOptionAliasesAndOptionValue() {
        TestServices services = new TestServices();
        CommandCompletionService completion = new CommandCompletionService(services);
        CommandDescriptor descriptor = new CommandParser().parse(new OptionedArenaCommand());

        List<String> optionAliases = completion.complete(descriptor, allowAll(), new String[]{"view", "--a"});
        assertEquals(Arrays.asList("--arena"), optionAliases);

        List<String> optionValue = completion.complete(descriptor, allowAll(), new String[]{"view", "--arena", "sk"});
        assertEquals(Arrays.asList("skywars"), optionValue);
    }

    @Test
    public void completesInlineLiteralSuggestions() {
        TestServices services = new TestServices();
        CommandCompletionService completion = new CommandCompletionService(services);
        CommandDescriptor descriptor = new CommandParser().parse(new InlineSuggestionCommand());

        List<String> out = completion.complete(descriptor, allowAll(), new String[]{"mode", "ra"});
        assertEquals(Arrays.asList("ranked"), out);
    }

    private static CompletionAdapter allowAll() {
        return new CompletionAdapter() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public Object getSender() {
                return "console";
            }
        };
    }

    @Definition(names = {"arena"})
    private static class SubOnlyCommand extends BaseCommand {

        @Subcommand(names = {"view"})
        public void view(CommandContext sender) {
        }

        @Subcommand(names = {"edit"})
        public void edit(CommandContext sender) {
        }
    }

    @Definition(names = {"arena"})
    private static class ArenaCommand extends BaseCommand {

        @Subcommand(names = {"view"})
        public void view(CommandContext sender, @Name("id") @Suggestions(TestArenaIds.class) String arenaId) {
        }
    }

    public static final class TestArenaIds implements SuggestionProvider {
        @Override
        public List<String> suggest(CompletionContext context) {
            return Arrays.asList("duel", "skywars", "lobby");
        }
    }

    @Definition(names = {"arena"})
    private static class OptionedArenaCommand extends BaseCommand {

        @Subcommand(names = {"view"})
        public void view(
                CommandContext sender,
                @Name("arena") @Option(names = {"--arena"}) @Suggestions(TestArenaIds.class) String arenaId,
                @Name("silent") @Flag(names = {"-s", "--silent"}) boolean silent
        ) {
        }
    }

    @Definition(names = {"show"})
    private static class InlineSuggestionCommand extends BaseCommand {
        @Subcommand(names = {"mode"})
        public void mode(CommandContext sender, @Name("mode") @Suggestions(literals = {"normal", "ranked", "casual"}) String mode) {
        }
    }

    private static class TestServices implements CommandExecutionServices {
        private final ParameterResolverRegistry registry = new ParameterResolverRegistry();
        private final ConcurrentHashMap<Class<? extends SuggestionProvider>, SuggestionProvider> suggestionProviders =
                new ConcurrentHashMap<>();

        TestServices() {
            BuiltinParameterResolvers.registerAll(this.registry);
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
                    return "";
                }

                @Override
                public String noPermissionForRoot(CommandDescriptor descriptor) {
                    return "";
                }

                @Override
                public String noPermissionForSubcommand(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor
                ) {
                    return "";
                }

                @Override
                public String unknownSubcommand(CommandDescriptor descriptor, String token, String availableSubcommands) {
                    return "";
                }

                @Override
                public String cooldownActive(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        long remainingMillis
                ) {
                    return "";
                }

                @Override
                public String conditionError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        CommandConditionException exception
                ) {
                    return "";
                }

                @Override
                public String noHandler(CommandDescriptor descriptor, String availableSubcommands) {
                    return "";
                }

                @Override
                public String bindingError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        CommandBindingException exception
                ) {
                    return "";
                }

                @Override
                public String executionError(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor,
                        RuntimeException exception
                ) {
                    return "";
                }
            };
        }

        @Override
        public CommandHelpFormatter getHelpFormatter() {
            return new CommandHelpFormatter() {
                @Override
                public List<String> renderGeneratedHelp(CommandDescriptor descriptor) {
                    return java.util.Collections.emptyList();
                }

                @Override
                public String renderRootUsage(CommandDescriptor descriptor, String availableSubcommands) {
                    return "";
                }

                @Override
                public String renderSubcommandUsage(
                        CommandDescriptor descriptor,
                        CommandMethodDescriptor methodDescriptor
                ) {
                    return "";
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
}
