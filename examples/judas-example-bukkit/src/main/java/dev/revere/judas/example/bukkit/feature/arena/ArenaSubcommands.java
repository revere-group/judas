package dev.revere.judas.example.bukkit.feature.arena;

import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.model.command.BaseCommand;
import dev.revere.judas.example.bukkit.ArenaNameSuggestions;
import org.bukkit.entity.Player;

/**
 * Subcommands defined in a different location, then linked to roots by registration API.
 *
 * <p>How to read this class:
 * <ul>
 *     <li>{@code view} has no explicit parent. It can be registered under any root passed to
 *     {@code registerSubcommands("root", holder)}.</li>
 *     <li>{@code create} has no explicit parent either, same behavior as {@code view}.</li>
 *     <li>{@code skipexample} explicitly declares {@code parent = "kit"}:
 *          <ul>
 *              <li>If you bulk-register this holder under {@code "arena"}, this one is skipped.</li>
 *              <li>If you target-register this alias under {@code "arena"}, registration fails.</li>
 *              <li>If you auto-parent register ({@code registerSubcommand(holder, "skipexample")}),
 *              it resolves to {@code "kit"} automatically.</li>
 *          </ul>
 *     </li>
 * </ul>
 */
public class ArenaSubcommands extends BaseCommand {

    @Subcommand(names = {"view", "v"})
    public void onView(
            @Sender Player player,
            @Name("arena") @Suggestions(ArenaNameSuggestions.class) String arenaId
    ) {
        player.sendMessage("Viewing arena: " + arenaId);
    }

    @Subcommand(names = {"create"})
    public void onCreate(
            @Sender Player player,
            @Name("arena") @Option(names = {"--name"}) String arenaId
    ) {
        player.sendMessage("Created arena: " + arenaId);
    }

    // Intentionally targets a different root to demonstrate parent-aware registration behavior.
    @Subcommand(names = {"skipexample"}, parent = "kit")
    public void onSkipExample(
            @Sender Player player
    ) {
        player.sendMessage("test");
    }
}
