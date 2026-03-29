package dev.revere.judas.api.context;

import dev.revere.judas.api.message.CommandColor;

/**
 * Runtime-agnostic view of a command invocation: sender, raw arguments, permissions, and messaging.
 */
public interface CommandContext {

    /**
     * @return raw argument tokens after the command label
     */
    String[] getArguments();

    /**
     * Sends a plain message to the command sender.
     *
     * @param message message text
     */
    void sendMessage(String message);

    /**
     * Sends a semantically colored message to the command sender.
     *
     * @param color semantic color bucket
     * @param message message text
     */
    void sendMessage(CommandColor color, String message);

    /**
     * Checks whether the sender is authorized for a permission node.
     *
     * @param permission permission node to check
     * @return {@code true} if the sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * @return display name of the sender where applicable
     */
    String getSenderName();

    /**
     * @return underlying platform sender instance
     */
    Object getSender();
}
