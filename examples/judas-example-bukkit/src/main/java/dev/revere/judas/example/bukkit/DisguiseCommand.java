package dev.revere.judas.example.bukkit;

import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Scenario: single-root command with classic subcommands plus advanced option parsing.
 *
 * <p>Demonstrates:
 * <ul>
 *     <li>Subcommand routing ({@code /disguise help}, {@code /disguise set ...}).</li>
 *     <li>Sender injection via {@link Sender}.</li>
 *     <li>Ordered positional args ({@code targets skin rank}).</li>
 *     <li>Unordered named options with aliases ({@code --skin}, {@code --rank}).</li>
 *     <li>Boolean flags ({@code -s}/{@code --silent}) that can appear anywhere.</li>
 * </ul>
 */
@Definition(names = {"disguise", "d"})
@Description("A command to disguise yourself.")
public class DisguiseCommand extends BaseCommand {

    @Subcommand(names = {"help"})
    public void onHelp(@Sender Player player) {
        player.sendMessage("Usage: /disguise set <targets> [--skin <skin>] [--rank <rank>] [-s|--silent]");
        player.sendMessage("You can still use positional order: /disguise set <targets> <skin> [rank]");
    }

    @Subcommand(names = {"set"})
    @Permission("judas.disguise.set")
    public void onSet(
            @Sender Player executor,
            @Name("targets") Player[] targets,
            // Can be positional OR explicitly passed as --skin/-k in any order.
            @Name("skin") @Option(names = {"-k", "--skin"}) String skin,
            // Optional with default; also supports named passing as --rank/-r.
            @Name("rank") @Option(names = {"-r", "--rank"}) @Optional @Default("Default") String rank,
            // Switch flag: if present, set to true; otherwise false.
            @Name("silent") @Flag(names = {"-s", "--silent"}) boolean silent
    ) {
        for (Player target : targets) {
            if (!silent) {
                target.sendMessage("You were disguised with skin " + skin + " and rank " + rank + " by " + executor.getName() + ".");
            }
        }

        executor.sendMessage("Applied disguise to " + targets.length + " player(s) with skin " + skin + " and rank " + rank + ".");
    }
}
