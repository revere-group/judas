package dev.revere.judas.example.bukkit.command.arena;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.example.bukkit.completion.ArenaIdSuggestions;
import dev.revere.judas.example.bukkit.model.Arena;
import dev.revere.judas.example.bukkit.service.ArenaService;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Root holder for arena feature.
 *
 * <p>Includes a shortcut root alias example:
 * {@code /arena view <arena>} can also be used as {@code /av <arena>}.
 * If {@code av} conflicts with any already-registered root alias, Judas fails registration with a clear exception.
 */
@Definition(names = {"arena", "a"}, generateHelp = true)
@Description("Arena management root command.")
public final class ArenaRootCommand extends BaseCommand {
    private final ArenaService arenaService;

    public ArenaRootCommand(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    public void onArenaRoot(@Sender Player sender) {
        sender.sendMessage("Use /arena help for arena commands.");
    }

    @Subcommand(names = {"view"}, parent = "arena")
    @Definition(names = {"av"})
    @Description("Shortcut root alias for /arena view <arena>.")
    public void viewShortcut(
            @Sender Player sender,
            @Name("arena") @Suggestions(ArenaIdSuggestions.class) String arenaId
    ) {
        Arena arena = this.arenaService.findById(arenaId);
        if (arena == null) {
            sender.sendMessage("Arena not found: " + arenaId);
            return;
        }
        sender.sendMessage("Arena " + arena.getId() + " mode=" + arena.getMode() + " enabled=" + arena.isEnabled());
    }
}
