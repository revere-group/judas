package dev.revere.judas.runtime;

import dev.revere.judas.model.spi.JudasLogger;

/**
 * Default JVM logger for Judas when no platform-specific logger is provided.
 */
public final class DefaultJudasLogger implements JudasLogger {
    private static final String PREFIX = "[Judas] ";

    @Override
    public void info(String message) {
        System.out.println(PREFIX + "[INFO] " + message);
    }

    @Override
    public void warn(String message) {
        System.out.println(PREFIX + "[WARN] " + message);
    }

    @Override
    public void debug(String message) {
        System.out.println(PREFIX + "[DEBUG] " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        System.err.println(PREFIX + "[ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
