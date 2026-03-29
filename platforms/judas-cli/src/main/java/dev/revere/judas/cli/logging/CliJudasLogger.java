package dev.revere.judas.cli.logging;

import dev.revere.judas.model.spi.JudasLogger;

/**
 * CLI-backed logger implementation for Judas internal framework logs.
 */
public final class CliJudasLogger implements JudasLogger {
    @Override
    public void info(String message) {
        System.out.println("[Judas] [INFO] " + message);
    }

    @Override
    public void warn(String message) {
        System.err.println("[Judas] [WARN] " + message);
    }

    @Override
    public void debug(String message) {
        System.out.println("[Judas] [DEBUG] " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        System.err.println("[Judas] [ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }
}
