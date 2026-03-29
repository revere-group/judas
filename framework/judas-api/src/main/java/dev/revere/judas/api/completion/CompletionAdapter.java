package dev.revere.judas.api.completion;

/**
 * Minimal sender-facing contract used by completion services.
 *
 * <p>This keeps completion logic runtime-agnostic: Bukkit, Bungee, Sponge, etc. can each adapt their
 * sender model without leaking platform classes into the engine.
 */
public interface CompletionAdapter {

    /**
     * Checks whether the sender can see/use candidates requiring the permission.
     *
     * @param permission permission node to validate
     * @return {@code true} when the sender has this permission
     */
    boolean hasPermission(String permission);

    /**
     * Underlying sender object (runtime-specific).
     *
     * @return sender instance used by custom completion providers
     */
    Object getSender();
}
