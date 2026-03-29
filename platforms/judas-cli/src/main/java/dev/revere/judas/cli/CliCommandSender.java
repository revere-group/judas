package dev.revere.judas.cli;

/**
 * Sender abstraction for CLI command execution.
 */
public interface CliCommandSender {

    /**
     * Delivers one output line to this sender.
     *
     * @param message output message
     */
    void sendMessage(String message);

    /**
     * Checks whether this sender is authorized for a permission node.
     *
     * @param permission permission node
     * @return {@code true} when this sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Returns a display name for logs and user-facing feedback.
     *
     * @return sender display name
     */
    String getName();
}
