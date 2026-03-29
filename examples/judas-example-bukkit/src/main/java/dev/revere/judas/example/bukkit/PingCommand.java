package dev.revere.judas.example.bukkit;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Scenario: root-only command (no subcommands).
 *
 * <p>Demonstrates a single primary command handler with an optional parameter:
 * <ul>
 *     <li>{@code /ping} -> sender ping.</li>
 *     <li>{@code /ping <target>} -> target ping.</li>
 * </ul>
 */
@Description("Checks ping for yourself or an optional target.")
public class PingCommand extends BaseCommand {

    @Definition(names = {"ping"})
    public void onPing(@Sender Player sender, @Name("target") @Optional Player target) {
        // If target is omitted, command logic falls back to sender.
        Player resolvedTarget = target != null ? target : sender;
        int ping = this.resolvePing(resolvedTarget);

        if (resolvedTarget.equals(sender)) {
            sender.sendMessage("Your ping: " + ping + "ms");
            return;
        }

        sender.sendMessage(resolvedTarget.getName() + "'s ping: " + ping + "ms");
    }

    private int resolvePing(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object value = handle.getClass().getField("ping").get(handle);
            return value instanceof Integer ? (Integer) value : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }
}
