package dev.revere.judas.example.bukkit;

import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Scenario: root-holder command.
 *
 * <p>This class only declares the primary root ({@code arena}) and default usage output.
 * Feature subcommands are intentionally defined in separate holders:
 * <ul>
 *     <li>{@code ArenaSubcommands}: linked by explicit root in {@code ExamplePlugin}.</li>
 *     <li>{@code ArenaExternalSubcommands}: linked by auto-parent resolution in {@code OutsideCommandRegistrar}.</li>
 * </ul>
 */
@Description("Arena command root. Use subcommands like /arena view <arena>.")
public class ArenaCommand extends BaseCommand {

    @Definition(names = {"arena", "a"})
    public void onRoot(@Sender Player player) {
        player.sendMessage("Usage: /arena <view|create|where> ...");
    }
}
