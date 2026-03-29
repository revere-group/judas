package dev.revere.judas.example.bukkit.command.arena;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.example.bukkit.model.Arena;
import dev.revere.judas.example.bukkit.service.ArenaService;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Arena feature subcommands, manually attached to arena root.
 */
public final class ArenaSubcommands extends BaseCommand {
    private final ArenaService arenaService;

    public ArenaSubcommands(ArenaService arenaService) {
        this.arenaService = arenaService;
    }

    @Subcommand(names = {"list"})
    @Description("Lists all arenas in memory.")
    public void list(@Sender Player sender) {
        sender.sendMessage("Arenas: " + this.arenaService.suggestIds(""));
    }

    @Subcommand(names = {"create"})
    @Permission("judas.example.arena.create")
    @Description("Creates an arena using options + defaults + consume remaining.")
    public void create(
            @Sender Player sender,
            @Name("arena") @Option(names = {"--id"}) @Conditions({"argument-not-empty"}) String arenaId,
            @Name("mode") @Option(names = {"--mode"}) @Optional @Default("casual") String mode,
            @Name("silent") @Flag(names = {"-s", "--silent"}) boolean silent,
            @Name("note") @ConsumeRemaining @Optional String note
    ) {
        this.arenaService.save(new Arena(arenaId, mode, true));
        if (!silent) {
            sender.sendMessage("Created arena '" + arenaId + "' mode=" + mode + " note=" + (note == null ? "<none>" : note));
        }
    }

    @Subcommand(names = {"feature"}, parent = "kit")
    public void kitParentOnly(@Sender Player sender) {
        // This stays in the arena holder on purpose so the example shows parent mismatch handling clearly.
        sender.sendMessage("This subcommand is parent-bound to /kit.");
    }
}
