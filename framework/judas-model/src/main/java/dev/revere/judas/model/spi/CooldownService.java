package dev.revere.judas.model.spi;

/**
 * Tracks command cooldown windows keyed by framework-generated identifiers.
 */
@FunctionalInterface
public interface CooldownService {
    CooldownService NO_OP = new CooldownService() {
        @Override
        public long acquireOrGetRemaining(String key, long cooldownMillis) {
            return 0L;
        }
    };

    /**
     * Attempts to start a cooldown window or returns remaining active time.
     *
     * <p>Contract:
     * return {@code 0} when execution is allowed and cooldown has been acquired;
     * return a positive duration when execution is denied by an active cooldown.
     *
     * @param key cooldown bucket key
     * @param cooldownMillis cooldown duration in milliseconds
     * @return remaining cooldown millis when still active, or {@code 0} when execution is allowed
     */
    long acquireOrGetRemaining(String key, long cooldownMillis);
}
