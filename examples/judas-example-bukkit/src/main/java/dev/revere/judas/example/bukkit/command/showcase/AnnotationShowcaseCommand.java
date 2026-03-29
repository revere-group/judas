package dev.revere.judas.example.bukkit.command.showcase;

import dev.revere.judas.api.annotation.ConsumeRemaining;
import dev.revere.judas.api.annotation.Default;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Flag;
import dev.revere.judas.api.annotation.Length;
import dev.revere.judas.api.annotation.Max;
import dev.revere.judas.api.annotation.Min;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Option;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Focused syntax showcase for option/flag ordering and consume remaining.
 */
@Definition(names = {"showcase"}, generateHelp = true)
@Description("Showcases option/flag syntax variants.")
public final class AnnotationShowcaseCommand extends BaseCommand {
    @Subcommand(names = {"demo"})
    public void demo(
            @Sender Player sender,
            @Name("mode") @Suggestions(literals = {"normal", "ranked", "casual"}) @Optional @Default("normal") String mode,
            @Name("priority") @Option(names = {"--priority", "-p"}) @Optional @Default("1") int priority,
            @Name("silent") @Flag(names = {"--silent", "-s"}) boolean silent,
            @Name("note") @ConsumeRemaining @Optional String note
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
            @Name("amount") @Range(min = 1, max = 64) int amount,
            @Name("minOnly") @Min(5) int minOnly,
            @Name("maxOnly") @Max(100) int maxOnly,
            @Name("tag") @Length(min = 3, max = 12) @Regex("^[a-zA-Z0-9_]+$") String tag
    ) {
        sender.sendMessage("validated -> amount=" + amount + ", minOnly=" + minOnly + ", maxOnly=" + maxOnly + ", tag=" + tag);
    }
}
