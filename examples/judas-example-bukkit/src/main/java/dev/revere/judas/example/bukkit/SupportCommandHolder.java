package dev.revere.judas.example.bukkit;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Scenario: multi-root holder (multiple primary commands in one class).
 *
 * <p>Demonstrates:
 * <ul>
 *     <li>Multiple roots in one holder via method-level {@code @Definition}.</li>
 *     <li>Subcommands explicitly bound to each root using {@code @Subcommand(parent = "...")}.</li>
 *     <li>A root ({@code kit}) declared specifically so parent-targeted subcommands from other holders
 *     can resolve and register there.</li>
 * </ul>
 */
public class SupportCommandHolder extends BaseCommand {

    @Definition(names = {"ticket"})
    public void ticketRoot(@Sender Player player) {
        player.sendMessage("Usage: /ticket open <subject>");
    }

    @Definition(names = {"report"})
    public void reportRoot(@Sender Player player) {
        player.sendMessage("Usage: /report status");
    }

    @Definition(names = {"kit"})
    public void kitRoot(@Sender Player player) {
        player.sendMessage("Usage: /kit <subcommand>");
    }

    @Subcommand(names = {"open"}, parent = "ticket")
    public void ticketOpen(@Sender Player player, @Name("subject") String subject) {
        player.sendMessage("Ticket opened with subject: " + subject);
    }

    @Subcommand(names = {"status"}, parent = "report")
    public void reportStatus(@Sender Player player) {
        player.sendMessage("Your latest report is still in review.");
    }
}
