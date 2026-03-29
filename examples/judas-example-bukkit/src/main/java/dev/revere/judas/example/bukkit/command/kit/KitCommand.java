package dev.revere.judas.example.bukkit.command.kit;

import dev.revere.judas.api.annotation.Cooldown;
import dev.revere.judas.api.annotation.CooldownScope;
import dev.revere.judas.api.annotation.Definition;
import dev.revere.judas.api.annotation.Description;
import dev.revere.judas.api.annotation.Name;
import dev.revere.judas.api.annotation.Optional;
import dev.revere.judas.api.annotation.Sender;
import dev.revere.judas.api.annotation.Subcommand;
import dev.revere.judas.api.annotation.Suggestions;
import dev.revere.judas.example.bukkit.completion.KitIdSuggestions;
import dev.revere.judas.example.bukkit.model.Kit;
import dev.revere.judas.example.bukkit.service.KitService;
import dev.revere.judas.model.command.BaseCommand;
import org.bukkit.entity.Player;

/**
 * Demonstrates both completion styles:
 * <ul>
 *     <li>{@code Kit} parameter uses resolver parsing + resolver completion.</li>
 *     <li>{@code String} parameter uses {@code @Suggestions(...)} provider.</li>
 * </ul>
 */
@Definition(names = {"kit"}, generateHelp = true)
@Description("Kit command demonstrating custom class resolver usage.")
public final class KitCommand extends BaseCommand {
    private final KitService kitService;

    public KitCommand(KitService kitService) {
        this.kitService = kitService;
    }

    @Subcommand(names = {"list"})
    public void list(@Sender Player sender) {
        sender.sendMessage("Available kits: " + this.kitService.suggestIds(""));
    }

    @Subcommand(names = {"getinventory"})
    public void getInventory(
            @Sender Player sender,
            @Name("kit") Kit kit
    ) {
        sender.sendMessage("Kit " + kit.getDisplayName() + " preview: " + kit.getInventoryPreview());
    }

    @Subcommand(names = {"equip"})
    @Cooldown(value = 3, scope = CooldownScope.SENDER)
    public void equip(
            @Sender Player sender,
            @Name("kit") Kit kit,
            @Name("target") @Optional Player target
    ) {
        Player resolved = target == null ? sender : target;
        resolved.sendMessage("You equipped kit " + kit.getDisplayName() + ".");
        if (!resolved.equals(sender)) {
            sender.sendMessage("Equipped " + resolved.getName() + " with " + kit.getDisplayName() + ".");
        }
    }

    @Subcommand(names = {"lookupraw"})
    public void lookupRaw(
            @Sender Player sender,
            @Name("kitId") @Suggestions(KitIdSuggestions.class) String kitId
    ) {
        Kit kit = this.kitService.findById(kitId);
        if (kit == null) {
            sender.sendMessage("Kit not found: " + kitId);
            return;
        }
        sender.sendMessage("Resolved raw id " + kitId + " -> " + kit.getDisplayName());
    }
}
