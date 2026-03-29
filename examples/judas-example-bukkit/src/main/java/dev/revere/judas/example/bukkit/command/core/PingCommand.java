package dev.revere.judas.example.bukkit.command.core;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Root-only command example with optional target argument.
 */
@Description("Checks your ping or another player's ping.")
public final class PingCommand extends BaseCommand {
    @Definition(names = {"ping"}, generateHelp = true)
    public void onPing(@Sender Player sender, @Name("target") @Optional Player target) {
        Player resolved = target == null ? sender : target;
        int ping = resolvePing(resolved);
        if (ping < 0) {
            sender.sendMessage("Could not resolve ping for " + resolved.getName() + " on this server version.");
            return;
        }
        sender.sendMessage(resolved.getName() + " ping: " + ping + "ms");
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
