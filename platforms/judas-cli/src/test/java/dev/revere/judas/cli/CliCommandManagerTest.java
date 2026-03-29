package dev.revere.judas.cli;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CliCommandManagerTest {

    @Test
    public void executesRegisteredRootAndSubcommand() {
        CliCommandManager manager = new CliCommandManager();
        EchoCommand command = new EchoCommand();
        manager.register(command);

        RecordingSender sender = new RecordingSender();
        boolean result = manager.execute(sender, "echo say hello");

        assertTrue(result);
        assertEquals("hello", command.lastMessage);
    }

    @Test
    public void reportsUnknownRoot() {
        CliCommandManager manager = new CliCommandManager();
        RecordingSender sender = new RecordingSender();

        boolean result = manager.execute(sender, "missing");

        assertFalse(result);
        assertEquals(1, sender.messages.size());
    }

    @Definition(names = {"echo"})
    private static final class EchoCommand extends BaseCommand {
        private String lastMessage;

        @Subcommand(names = {"say"})
        public void say(@Name("message") String message) {
            this.lastMessage = message;
        }
    }

    private static final class RecordingSender implements CliCommandSender {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendMessage(String message) {
            this.messages.add(message);
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public String getName() {
            return "console";
        }
    }
}
