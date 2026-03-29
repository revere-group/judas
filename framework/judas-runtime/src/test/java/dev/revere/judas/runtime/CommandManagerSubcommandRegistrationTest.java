package dev.revere.judas.runtime;

import dev.revere.judas.api.annotation.RootCommand;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.model.exception.SubcommandParentMismatchException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommandManagerSubcommandRegistrationTest {

    @Test
    public void explicitRootBulkRegistrationSkipsDifferentParent() {
        TestManager manager = new TestManager();
        manager.register(new RootHolder());

        manager.registerSub("arena", new MixedParentSubcommands());

        CommandDescriptor arena = manager.getCommands().get("arena");
        assertEquals(1, arena.getSubcommands().size());
        assertEquals("view", arena.getSubcommands().get(0).getNames()[0]);
    }

    @Test
    public void explicitRootTargetedRegistrationFailsForDifferentParent() {
        TestManager manager = new TestManager();
        manager.register(new RootHolder());

        try {
            manager.registerSub("arena", new MixedParentSubcommands(), "skipexample");
            fail("Expected registration to fail due to parent mismatch.");
        } catch (SubcommandParentMismatchException expected) {
            assertTrue(expected.getMessage().contains("cannot be registered under root"));
        }
    }

    @Test
    public void autoParentTargetedRegistrationUsesDeclaredParent() {
        TestManager manager = new TestManager();
        manager.register(new RootHolder());

        manager.registerSub(new MixedParentSubcommands(), "skipexample");

        CommandDescriptor kit = manager.getCommands().get("kit");
        assertEquals(1, kit.getSubcommands().size());
        assertEquals("skipexample", kit.getSubcommands().get(0).getNames()[0]);
    }

    private static class TestManager extends CommandManager {
        @Override
        protected void registerPlatform(CommandDescriptor descriptor) {
        }
    }

    private static class RootHolder extends BaseCommand {
        @RootCommand(names = {"arena"})
        public void arenaRoot(@Sender Object sender) {
        }

        @RootCommand(names = {"kit"})
        public void kitRoot(@Sender Object sender) {
        }
    }

    private static class MixedParentSubcommands extends BaseCommand {
        @Subcommand(names = {"view"}, parent = "arena")
        public void onView(@Sender Object sender) {
        }

        @Subcommand(names = {"skipexample"}, parent = "kit")
        public void onSkip(@Sender Object sender) {
        }
    }
}
