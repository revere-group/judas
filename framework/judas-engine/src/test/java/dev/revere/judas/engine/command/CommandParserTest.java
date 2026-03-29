package dev.revere.judas.engine.command;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Length;
import dev.revere.judas.api.annotation.Max;
import dev.revere.judas.api.annotation.Min;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.engine.command.metadata.CommandParser;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.command.CommandMethodDescriptor;
import dev.revere.judas.model.command.ParameterDescriptor;
import dev.revere.judas.model.exception.SubcommandShortcutAliasConflictException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandParserTest {

    @Test
    public void parsesSubcommandParameterMetadata() {
        CommandParser parser = new CommandParser();
        CommandDescriptor descriptor = parser.parse(new ParserCommand());

        assertArrayEquals(new String[]{"sample", "s"}, descriptor.getNames());
        assertEquals("judas.sample", descriptor.getPermission());
        assertEquals(1, descriptor.getSubcommands().size());

        CommandMethodDescriptor methodDescriptor = descriptor.getSubcommands().get(0);
        assertArrayEquals(new String[]{"set"}, methodDescriptor.getNames());
        assertEquals("judas.sample.set", methodDescriptor.getPermission());
        assertEquals(3, methodDescriptor.getParameters().size());

        ParameterDescriptor first = methodDescriptor.getParameters().get(0);
        assertEquals("target", first.getName());
        assertEquals(String.class, first.getType());
        assertFalse(first.isOptional());

        ParameterDescriptor second = methodDescriptor.getParameters().get(1);
        assertTrue(second.isOptional());
        assertEquals("Default", second.getDefaultValue());

        ParameterDescriptor third = methodDescriptor.getParameters().get(2);
        assertTrue(third.isConsumeRemaining());
        assertTrue(methodDescriptor.getParameters().stream().allMatch(p -> p.getSuggestionProviderType() == null));
    }

    @Test
    public void parsesMultiRootHolderWithExplicitSubcommandParent() {
        CommandParser parser = new CommandParser();
        List<CommandDescriptor> roots = parser.parseAll(new MultiRootHolder());

        assertEquals(2, roots.size());

        CommandDescriptor ping = findRoot(roots, "ping");
        CommandDescriptor arena = findRoot(roots, "arena");

        assertEquals("holder.ping", ping.getPermission());
        assertEquals(1, ping.getSubcommands().size());
        assertArrayEquals(new String[]{"stats"}, ping.getSubcommands().get(0).getNames());

        assertEquals("holder.arena", arena.getPermission());
        assertEquals(1, arena.getSubcommands().size());
        assertArrayEquals(new String[]{"view"}, arena.getSubcommands().get(0).getNames());
    }

    @Test
    public void parsesSubcommandShortcutRootUsingDefinitionPlusSubcommand() {
        CommandParser parser = new CommandParser();
        List<CommandDescriptor> roots = parser.parseAll(new ShortcutHolder());

        assertEquals(2, roots.size());

        CommandDescriptor report = findRoot(roots, "report");
        CommandDescriptor shortcut = findRoot(roots, "rs");

        assertEquals(1, report.getSubcommands().size());
        assertArrayEquals(new String[]{"status"}, report.getSubcommands().get(0).getNames());
        assertEquals(0, shortcut.getSubcommands().size());
        assertEquals("report.status", shortcut.getDefaultMethod().getPermission());
    }

    @Test(expected = SubcommandShortcutAliasConflictException.class)
    public void failsWhenShortcutAliasCollidesWithPrimaryAlias() {
        CommandParser parser = new CommandParser();
        parser.parseAll(new ShortcutCollisionHolder());
    }

    @Test
    public void parsesGenerateHelpOnRootDefinition() {
        CommandParser parser = new CommandParser();
        CommandDescriptor descriptor = parser.parse(new HelpEnabledHolder());
        assertTrue(descriptor.isGenerateHelp());
    }

    @Test
    public void parsesConditionsAcrossRootMethodAndParameter() {
        CommandParser parser = new CommandParser();
        CommandDescriptor descriptor = parser.parse(new ConditionHolder());

        assertEquals(1, descriptor.getConditions().size());
        assertEquals("root-only", descriptor.getConditions().get(0));
        CommandMethodDescriptor method = descriptor.getSubcommands().get(0);
        assertEquals("method-only", method.getConditions().get(0));
        ParameterDescriptor parameter = method.getParameters().get(0);
        assertEquals("param-only", parameter.getConditions().get(0));
    }

    @Test
    public void parsesValidationAnnotationsIntoConditions() {
        CommandParser parser = new CommandParser();
        CommandDescriptor descriptor = parser.parse(new ValidationHolder());
        ParameterDescriptor numeric = descriptor.getSubcommands().get(0).getParameters().get(0);
        ParameterDescriptor text = descriptor.getSubcommands().get(0).getParameters().get(1);

        assertTrue(numeric.getConditions().stream().anyMatch(s -> s.startsWith("range:")));
        assertTrue(numeric.getConditions().stream().anyMatch(s -> s.startsWith("min:")));
        assertTrue(numeric.getConditions().stream().anyMatch(s -> s.startsWith("max:")));
        assertTrue(text.getConditions().stream().anyMatch(s -> s.startsWith("length:")));
        assertTrue(text.getConditions().stream().anyMatch(s -> s.startsWith("regex:")));
    }

    private CommandDescriptor findRoot(List<CommandDescriptor> roots, String alias) {
        for (CommandDescriptor root : roots) {
            for (String name : root.getNames()) {
                if (name.equalsIgnoreCase(alias)) {
                    return root;
                }
            }
        }
        throw new AssertionError("Root not found: " + alias);
    }

    @Definition(names = {"sample", "s"})
    @Permission("judas.sample")
    private static class ParserCommand extends BaseCommand {

        @Subcommand(names = {"set"})
        @Permission("judas.sample.set")
        public void onSet(
                @Name("target") String target,
                @Name("rank") @Optional @Default("Default") String rank,
                @Name("message") @ConsumeRemaining String message
        ) {
        }
    }

    private static class MultiRootHolder extends BaseCommand {
        @Definition(names = {"ping"})
        @Permission("holder.ping")
        public void ping() {
        }

        @Definition(names = {"arena"})
        @Permission("holder.arena")
        public void arena() {
        }

        @Subcommand(names = {"stats"}, parent = "ping")
        public void pingStats() {
        }

        @Subcommand(names = {"view"}, parent = "arena")
        public void arenaView(@Name("id") String id) {
        }
    }

    private static class ShortcutHolder extends BaseCommand {
        @Definition(names = {"report"})
        public void reportRoot() {
        }

        @Subcommand(names = {"status"}, parent = "report")
        @Definition(names = {"rs"})
        @Permission("report.status")
        public void statusShortcut() {
        }
    }

    private static class ShortcutCollisionHolder extends BaseCommand {
        @Definition(names = {"report"})
        public void reportRoot() {
        }

        @Subcommand(names = {"status"}, parent = "report")
        @Definition(names = {"report"})
        public void invalidShortcut() {
        }
    }

    @Definition(names = {"arena"}, generateHelp = true)
    private static class HelpEnabledHolder extends BaseCommand {
        @Subcommand(names = {"view"})
        public void view() {
        }
    }

    @Definition(names = {"cond"})
    @Conditions({"root-only"})
    private static class ConditionHolder extends BaseCommand {
        @Subcommand(names = {"check"})
        @Conditions({"method-only"})
        public void check(@Name("value") @Conditions({"param-only"}) String value) {
        }
    }

    @Definition(names = {"validate"})
    private static class ValidationHolder extends BaseCommand {
        @Subcommand(names = {"test"})
        public void test(
                @Name("amount") @Range(min = 1, max = 10) @Min(1) @Max(10) int amount,
                @Name("code") @Length(min = 3, max = 8) @Regex("^[a-z]+$") String code
        ) {
        }
    }
}
