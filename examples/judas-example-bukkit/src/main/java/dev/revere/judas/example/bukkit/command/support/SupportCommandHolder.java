package dev.revere.judas.example.bukkit.command.support;

import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Multi-root holder showing explicit parent linking.
 */
public final class SupportCommandHolder extends BaseCommand {
    @Definition(names = {"report"}, generateHelp = true)
    @Description("Report issue tracking root.")
    public void reportRoot(@Sender Player sender) {
        sender.sendMessage("Use /report open <reason> or /report status.");
    }

    @Definition(names = {"ticket"}, generateHelp = true)
    @Description("Support ticket root.")
    public void ticketRoot(@Sender Player sender) {
        sender.sendMessage("Use /ticket create <subject>.");
    }

    @Subcommand(names = {"open"}, parent = "report")
    @Permission("judas.example.report.open")
    @Conditions({"player-only"})
    public void reportOpen(@Sender Player sender, @Name("reason") @Conditions({"argument-not-empty"}) String reason) {
        sender.sendMessage("Report submitted with reason: " + reason);
    }

    @Subcommand(names = {"status"}, parent = "report")
    public void reportStatus(@Sender Player sender) {
        sender.sendMessage("Latest report status: pending review.");
    }

    @Subcommand(names = {"create"}, parent = "ticket")
    public void ticketCreate(@Sender Player sender, @Name("subject") String subject) {
        sender.sendMessage("Ticket created with subject: " + subject);
    }
}
