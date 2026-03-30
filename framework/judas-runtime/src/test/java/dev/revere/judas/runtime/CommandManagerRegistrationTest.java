package dev.revere.judas.runtime;

import dev.revere.judas.api.annotation.RootCommand;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Shortcut;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.context.CommandContext;
import dev.revere.judas.api.message.CommandColor;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.exception.DuplicateCommandException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommandManagerRegistrationTest {

    @Test
    public void duplicateRootRegistrationIncludesExistingAndIncomingSourceDetails() {
        TestManager manager = new TestManager();
        manager.register(new ArenaHolder());

        try {
            manager.register(new ArenaAliasCollisionHolder());
            fail("Expected duplicate root registration failure.");
        } catch (DuplicateCommandException exception) {
            assertTrue(exception.getMessage().contains("Command alias 'a' is already registered during startup"));
            assertTrue(exception.getMessage().contains("Existing aliases=[arena, a]"));
            assertTrue(exception.getMessage().contains("incoming aliases=[arenaadmin, a]"));
            assertTrue(exception.getMessage().contains(ArenaHolder.class.getName()));
            assertTrue(exception.getMessage().contains(ArenaAliasCollisionHolder.class.getName()));
        }
    }

    @Test
    public void dispatchesClassLevelRootDefaultMethodWhenNoSubcommandIsProvided() {
        TestManager manager = new TestManager();
        ImplicitDefaultArenaHolder holder = new ImplicitDefaultArenaHolder();
        manager.register(holder);

        CommandDescriptor arena = manager.getCommands().get("arena");
        assertNotNull(arena);
        assertNotNull(arena.getDefaultMethod());

        TestContext context = new TestContext(new String[0]);
        manager.dispatch(arena, context);

        assertTrue(holder.invoked);
        assertFalse(context.messages.isEmpty());
        assertTrue(context.messages.get(0).contains("default arena root"));
    }

    @Test
    public void registersRootAndBulkSubcommandsWithExplicitRootAndSkipsDifferentParent() {
        TestManager manager = new TestManager();
        manager.register(new FlowArenaRootCommand());
        manager.registerSub("arena", new FlowArenaSubcommands());

        CommandDescriptor arena = manager.getCommands().get("arena");
        CommandDescriptor shortcut = manager.getCommands().get("av");
        assertNotNull(arena);
        assertNotNull(shortcut);
        assertNotNull(shortcut.getDefaultMethod());
        assertEquals("viewShortcut", shortcut.getDefaultMethod().getMethod().getName());

        assertTrue(hasSubcommand(arena, "view"));
        assertTrue(hasSubcommand(arena, "list"));
        assertTrue(hasSubcommand(arena, "create"));
        assertFalse(hasSubcommand(arena, "feature"));
        assertEquals(3, arena.getSubcommands().size());
    }

    private static class TestManager extends CommandManager {
        @Override
        protected void registerPlatform(CommandDescriptor descriptor) {
        }
    }

    private static class ArenaHolder extends BaseCommand {
        @RootCommand(names = {"arena", "a"})
        public void arena(@Sender Object sender) {
        }
    }

    private static class ArenaAliasCollisionHolder extends BaseCommand {
        @RootCommand(names = {"arenaadmin", "a"})
        public void arenaAdmin(@Sender Object sender) {
        }
    }

    @RootCommand(names = {"arena", "a"})
    private static class FlowArenaRootCommand extends BaseCommand {
        public void onArenaRoot(@Sender Object sender) {
        }

        @Subcommand(names = {"view"})
        @Shortcut(names = {"av"})
        public void viewShortcut(@Sender Object sender) {
        }
    }

    private static class FlowArenaSubcommands extends BaseCommand {
        @Subcommand(names = {"list"})
        public void list(@Sender Object sender) {
        }

        @Subcommand(names = {"create"})
        public void create(@Sender Object sender) {
        }

        @Subcommand(names = {"feature"}, parent = "kit")
        public void kitParentOnly(@Sender Object sender) {
        }
    }

    @RootCommand(names = {"arena"})
    private static class ImplicitDefaultArenaHolder extends BaseCommand {
        private boolean invoked;

        public void onArenaRoot(@Sender Object sender) {
            this.invoked = true;
            if (sender instanceof TestSender) {
                ((TestSender) sender).context.sendMessage("default arena root");
            }
        }

        @Subcommand(names = {"view"})
        public void onView() {
        }
    }

    private static class TestContext implements CommandContext {
        private final String[] args;
        private final List<String> messages = new ArrayList<>();
        private final TestSender sender = new TestSender(this);

        private TestContext(String[] args) {
            this.args = args;
        }

        @Override
        public String[] getArguments() {
            return this.args;
        }

        @Override
        public void sendMessage(String message) {
            this.messages.add(message);
        }

        @Override
        public void sendMessage(CommandColor color, String message) {
            this.messages.add(message);
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public String getSenderName() {
            return "tester";
        }

        @Override
        public Object getSender() {
            return this.sender;
        }
    }

    private static final class TestSender {
        private final TestContext context;

        private TestSender(TestContext context) {
            this.context = context;
        }
    }

    private static boolean hasSubcommand(CommandDescriptor root, String alias) {
        for (dev.revere.judas.model.command.CommandMethodDescriptor method : root.getSubcommands()) {
            for (String name : method.getNames()) {
                if (name.equalsIgnoreCase(alias)) {
                    return true;
                }
            }
        }
        return false;
    }
}
