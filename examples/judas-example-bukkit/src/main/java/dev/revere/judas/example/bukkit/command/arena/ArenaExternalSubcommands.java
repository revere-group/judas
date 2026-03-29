package dev.revere.judas.example.bukkit.command.arena;

import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Demonstrates auto-parent registration from outside main plugin bootstrap.
 */
public final class ArenaExternalSubcommands extends BaseCommand {
    @Subcommand(names = {"where"}, parent = "arena")
    @Description("Simple auto-parented subcommand from external registrar.")
    public void where(@Sender Player sender) {
        sender.sendMessage("You are in demo arena region.");
    }
}
