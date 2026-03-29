package dev.revere.judas.example.cli.command;

import dev.revere.judas.api.annotation.Async;
import dev.revere.judas.api.annotation.Conditions;
import dev.revere.judas.api.annotation.Cooldown;
import dev.revere.judas.api.annotation.CooldownScope;
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
import dev.revere.judas.api.annotation.Permission;
import dev.revere.judas.api.annotation.Range;
import dev.revere.judas.api.annotation.Regex;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.cli.CliCommandSender;
import dev.revere.judas.model.command.BaseCommand;

/**
 * One command that demonstrates every framework annotation in the CLI example.
 */
@Definition(names = {"cli-showcase", "cs"}, generateHelp = true)
@Description("Demonstrates every Judas annotation in one CLI command.")
@Permission("judas.example.cli")
@Conditions({"sender-name-not-empty"})
public final class CliAnnotationShowcaseCommand extends BaseCommand {

    @Subcommand(names = {"announce"})
    @Description("Runs CLI annotation showcase.")
    @Permission("judas.example.cli.announce")
    @Cooldown(value = 2, scope = CooldownScope.SENDER)
    public void announce(
            @Sender CliCommandSender sender,
            @Name("channel")
            @Suggestions(CliShowcaseSuggestions.class)
            @Conditions({"argument-not-empty"})
            String channel,
            @Name("level") @Suggestions(literals = {"info", "warn", "error"}) @Optional @Default("info") String level,
            @Name("times") @Option(names = {"--times", "-t"}) @Optional @Default("1") int times,
            @Name("silent") @Flag(names = {"--silent", "-s"}) boolean silent,
            @Name("message") @ConsumeRemaining @Optional String message
    ) {
        if (silent) {
            return;
        }
        String text = message == null ? "<empty>" : message;
        sender.sendMessage("announce -> channel=" + channel + ", level=" + level + ", times=" + times + ", message=" + text);
    }

    @Subcommand(names = {"validate"})
    @Description("Demonstrates @Range, @Min, @Max, @Length and @Regex validations.")
    public String validate(
            @Name("amount") @Range(min = 1, max = 64) int amount,
            @Name("minOnly") @Min(5) int minOnly,
            @Name("maxOnly") @Max(100) int maxOnly,
            @Name("tag") @Length(min = 3, max = 12) @Regex("^[a-zA-Z0-9_]+$") String tag
    ) {
        return "validated -> amount=" + amount + ", minOnly=" + minOnly + ", maxOnly=" + maxOnly + ", tag=" + tag;
    }

    @Subcommand(names = {"async"})
    @Async
    @Description("Demonstrates async execution with response handling.")
    public String async(@Name("task") @Optional @Default("refresh-cache") String task) {
        return "async -> completed task=" + task;
    }
}
