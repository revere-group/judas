package dev.revere.judas.bukkit.command;

import dev.revere.judas.api.completion.CompletionAdapter;
import dev.revere.judas.bukkit.context.BukkitCommandContext;
import dev.revere.judas.model.command.CommandDescriptor;
import dev.revere.judas.runtime.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Bukkit {@link Command} adapter that executes and tab-completes a Judas {@link CommandDescriptor}.
 */
public class BukkitCommand extends Command {
    private final CommandDescriptor descriptor;
    private final CommandManager commandManager;

    /**
     * @param descriptor Judas root descriptor
     * @param commandManager owning runtime manager
     */
    public BukkitCommand(CommandDescriptor descriptor, CommandManager commandManager) {
        super(descriptor.getNames()[0]);
        this.descriptor = descriptor;
        this.commandManager = commandManager;

        if (descriptor.getNames().length > 1) {
            this.setAliases(Arrays.asList(Arrays.copyOfRange(descriptor.getNames(), 1, descriptor.getNames().length)));
        }

        if (descriptor.getDescription() != null) {
            this.setDescription(descriptor.getDescription());
        }

        if (descriptor.getPermission() != null) {
            this.setPermission(descriptor.getPermission());
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        BukkitCommandContext context = new BukkitCommandContext(sender, args);
        this.commandManager.dispatch(this.descriptor, context);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        CompletionAdapter adapter = new CompletionAdapter() {
            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public Object getSender() {
                return sender;
            }
        };
        List<String> result = this.commandManager.complete(
                this.descriptor,
                adapter,
                args == null ? new String[0] : args
        );
        return result == null ? Collections.emptyList() : result;
    }
}
