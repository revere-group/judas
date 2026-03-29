package dev.revere.judas.example.bukkit.command.showcase;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.DefaultValue;
import dev.revere.judas.api.annotation.RootCommand;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Length;
import dev.revere.judas.api.annotation.Max;
import dev.revere.judas.api.annotation.Min;
import dev.revere.judas.api.annotation.Arg;
import dev.revere.judas.api.annotation.Switch;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Focused syntax showcase for valued switches, boolean flags, and consume-remaining ordering.
 */
@RootCommand(names = {"showcase"}, generateHelp = true)
@Description("Showcases switch/flag syntax variants.")
public final class AnnotationShowcaseCommand extends BaseCommand {
    @Subcommand(names = {"demo"})
    public void demo(
            @Sender Player sender,
            @Arg("mode") @Suggestions(literals = {"normal", "ranked", "casual"}) @Optional @DefaultValue("normal") String mode,
            @Arg("priority") @Switch(names = {"--priority", "-p"}) @Optional @DefaultValue("1") int priority,
            @Arg("silent") @Flag(names = {"--silent", "-s"}) boolean silent,
            @Arg("note") @ConsumeRemaining @Optional String note
    ) {
        if (silent) {
            return;
        }
        sender.sendMessage("mode=" + mode + ", priority=" + priority + ", note=" + (note == null ? "<none>" : note));
    }

    @Subcommand(names = {"validate"})
    @Description("Demonstrates @Range, @Min, @Max, @Length and @Regex validations.")
    public void validate(
            @Sender Player sender,
            @Arg("amount") @Range(min = 1, max = 64) int amount,
            @Arg("minOnly") @Min(5) int minOnly,
            @Arg("maxOnly") @Max(100) int maxOnly,
            @Arg("tag") @Length(min = 3, max = 12) @Regex("^[a-zA-Z0-9_]+$") String tag
    ) {
        sender.sendMessage("validated -> amount=" + amount + ", minOnly=" + minOnly + ", maxOnly=" + maxOnly + ", tag=" + tag);
    }
}
