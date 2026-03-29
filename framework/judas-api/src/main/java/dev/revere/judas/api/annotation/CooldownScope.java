package dev.revere.judas.api.annotation;

/**
 * Cooldown key scoping strategy.
 */
public enum CooldownScope {
    /**
     * One shared cooldown for all senders and usages.
     */
    GLOBAL,
    /**
     * One cooldown per root command.
     */
    ROOT,
    /**
     * One cooldown per root + subcommand/method.
     */
    METHOD,
    /**
     * One cooldown per sender and method.
     */
    SENDER
}
