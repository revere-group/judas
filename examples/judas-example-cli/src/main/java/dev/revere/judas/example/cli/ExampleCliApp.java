package dev.revere.judas.example.cli;

import dev.revere.judas.cli.CliCommandManager;
import dev.revere.judas.cli.CliCommandSender;
import dev.revere.judas.example.cli.command.CliAnnotationShowcaseCommand;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.runtime.CommandManagerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Minimal CLI application proving Judas runtime abstraction outside Minecraft.
 */
public final class ExampleCliApp {

    private ExampleCliApp() {
    }

    public static void main(String[] args) throws IOException {
        CommandManagerOptions options = CommandManagerOptions.builder()
                .messageProvider(new CliExampleMessageProvider())
                .build();
        CliCommandManager commandManager = new CliCommandManager(options);
        commandManager.registerCondition("sender-name-not-empty", context -> {
            String senderName = context.getCommandContext().getSenderName();
            if (senderName == null || senderName.trim().isEmpty()) {
                throw new CommandConditionException("Sender name must not be empty.");
            }
        });
        commandManager.registerCondition("argument-not-empty", context -> {
            Object value = context.getParameterValue();
            if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                throw new CommandConditionException("Value must not be empty.");
            }
        });
        commandManager.register(new CliAnnotationShowcaseCommand());

        CliCommandSender sender = new CliCommandSender() {
            @Override
            public void sendMessage(String message) {
                System.out.println(message);
            }

            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public String getName() {
                return "console";
            }
        };

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Judas CLI example started. Type commands or 'exit'.");
        while (true) {
            String line = reader.readLine();
            if (line == null || "exit".equalsIgnoreCase(line.trim())) {
                break;
            }
            commandManager.execute(sender, line);
        }
    }
}
