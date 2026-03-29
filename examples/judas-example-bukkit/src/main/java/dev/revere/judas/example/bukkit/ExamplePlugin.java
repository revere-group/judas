package dev.revere.judas.example.bukkit;

import dev.revere.judas.bukkit.BukkitCommandManager;
import dev.revere.judas.example.bukkit.bootstrap.ExternalCommandRegistrar;
import dev.revere.judas.example.bukkit.command.arena.ArenaRootCommand;
import dev.revere.judas.example.bukkit.command.arena.ArenaSubcommands;
import dev.revere.judas.example.bukkit.command.core.PingCommand;
import dev.revere.judas.example.bukkit.command.kit.KitCommand;
import dev.revere.judas.example.bukkit.command.showcase.AnnotationShowcaseCommand;
import dev.revere.judas.example.bukkit.completion.ArenaIdSuggestions;
import dev.revere.judas.example.bukkit.completion.KitIdSuggestions;
import dev.revere.judas.example.bukkit.model.Kit;
import dev.revere.judas.example.bukkit.resolver.KitParameterResolver;
import dev.revere.judas.example.bukkit.service.ArenaService;
import dev.revere.judas.example.bukkit.service.KitService;
import dev.revere.judas.model.condition.CommandConditionException;
import dev.revere.judas.model.condition.ConditionContext;
import dev.revere.judas.runtime.CommandManagerOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Example plugin entry point that demonstrates every supported manual registration scenario.
 *
 * <p>Scenarios shown here:
 * <ul>
 *     <li>Registering full root-command holders with {@code register(...)}.</li>
 *     <li>Registering all subcommands from another holder under an explicit root.</li>
 *     <li>Registering selected subcommands under an explicit root.</li>
 *     <li>Auto-resolving root from {@code @Subcommand(parent = "...")}.</li>
 *     <li>Registering commands from outside this class via manager access.</li>
 * </ul>
 */
public class ExamplePlugin extends JavaPlugin {
    private BukkitCommandManager commandManager;
    private KitService kitService;
    private ArenaService arenaService;

    @Override
    public void onEnable() {
        this.kitService = new KitService();
        this.arenaService = new ArenaService();

        // Core framework entry point for Bukkit with custom message styling and default help pipeline.
        CommandManagerOptions options = CommandManagerOptions.builder()
                .messageProvider(new BukkitExampleMessageProvider())
                .build();
        this.commandManager = new BukkitCommandManager(this, options);

        this.registerConditions();
        this.registerResolvers();
        this.registerSuggestionProviders();
        this.registerCoreCommands();

        // Register from another class to demonstrate modular bootstrap.
        ExternalCommandRegistrar.register(this);
    }

    private void registerConditions() {
        this.commandManager.registerCondition("player-only", this::validatePlayerSender);
        this.commandManager.registerCondition("argument-not-empty", context -> {
            Object value = context.getParameterValue();
            if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                throw new CommandConditionException("Value must not be empty.");
            }
        });
    }

    private void registerResolvers() {
        // Resolver = typed parsing + typed completion for the Kit class.
        // Commands with Kit params do not need @Suggestions.
        this.commandManager.registerResolver(Kit.class, new KitParameterResolver(this.kitService));
    }

    private void registerSuggestionProviders() {
        // Suggestion providers are mainly for String parameters where parsing stays String-based.
        this.commandManager.registerSuggestionProvider(ArenaIdSuggestions.class, new ArenaIdSuggestions(this.arenaService));
        this.commandManager.registerSuggestionProvider(KitIdSuggestions.class, new KitIdSuggestions(this.kitService));
    }

    private void registerCoreCommands() {
        this.commandManager.register(new PingCommand());
        this.commandManager.register(new KitCommand(this.kitService));
        this.commandManager.register(new ArenaRootCommand(this.arenaService));
        this.commandManager.register(new AnnotationShowcaseCommand());

        // Arena subcommands are registered from a separate holder under explicit root.
        this.commandManager.registerSubcommands("arena", new ArenaSubcommands(this.arenaService));
    }

    private void validatePlayerSender(ConditionContext context) {
        if (!(context.getCommandContext().getSender() instanceof Player)) {
            throw new CommandConditionException("Only players can use this command.");
        }
    }

    /**
     * @return initialized example command manager
     */
    public BukkitCommandManager getCommandManager() {
        return this.commandManager;
    }
}
