package dev.revere.judas.runtime;

import dev.revere.judas.model.spi.CooldownService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory cooldown service.
 */
public final class InMemoryCooldownService implements CooldownService {
    private final Map<String, Long> cooldownExpiryByKey = new ConcurrentHashMap<>();

    @Override
    public long acquireOrGetRemaining(String key, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long expiry = this.cooldownExpiryByKey.get(key);
        if (expiry != null && expiry > now) {
            return expiry - now;
        }
        this.cooldownExpiryByKey.put(key, now + cooldownMillis);
        return 0L;
    }
}
