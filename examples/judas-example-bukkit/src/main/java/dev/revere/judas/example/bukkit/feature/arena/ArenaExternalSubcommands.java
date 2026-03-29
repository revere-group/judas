package dev.revere.judas.example.bukkit.feature.arena;

import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Scenario: subcommands owned by a feature package and wired from outside main bootstrap.
 *
 * <p>{@code parent = "arena"} enables auto-root registration:
 * manager can link this without passing root explicitly.
 */
public class ArenaExternalSubcommands extends BaseCommand {

    @Subcommand(names = {"where"}, parent = "arena")
    public void onWhere(@Sender Player player) {
        player.sendMessage("You are currently in arena: demo");
    }
}
