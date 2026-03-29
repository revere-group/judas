package dev.revere.judas.model.spi;

/**
 * Internal framework logger abstraction used by Judas runtime/components.
 *
 * <p>This logger is for initialization, diagnostics, and framework lifecycle logs.
 * It is intentionally separate from user-facing command feedback messages.
 */
public interface JudasLogger {

    /**
     * Logs an informational framework message.
     *
     * @param message info message
     */
    void info(String message);

    /**
     * Logs a warning-level framework message.
     *
     * @param message warning message
     */
    void warn(String message);

    /**
     * Logs a debug-level framework message.
     *
     * @param message debug message
     */
    void debug(String message);

    /**
     * Logs an error-level framework message with optional cause.
     *
     * @param message error message
     * @param throwable optional failure cause (may be {@code null})
     */
    void error(String message, Throwable throwable);
}
