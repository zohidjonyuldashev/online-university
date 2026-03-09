package uz.pdp.online_university.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.enums.PolicyKey;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory cache of the latest policy values.
 *
 * <p>
 * Populated eagerly on startup by {@link PolicyService} and evicted/refreshed
 * on every write. Calculation services (e.g. attendance checks) should call
 * {@code PolicyCache.getValue(key)} instead of hitting the database on every
 * request.
 */
@Slf4j
@Component
public class PolicyCache {

    private final Map<PolicyKey, String> store = new EnumMap<>(PolicyKey.class);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /** Put or replace the cached value for a policy key. */
    public void put(PolicyKey key, String valueJson) {
        lock.writeLock().lock();
        try {
            store.put(key, valueJson);
            log.debug("PolicyCache updated: key={}, value={}", key, valueJson);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Remove a single key from the cache (forces next read to hit the DB). */
    public void evict(PolicyKey key) {
        lock.writeLock().lock();
        try {
            store.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the cached value for a key.
     *
     * @return {@code Optional.empty()} if the key has not been seeded yet.
     */
    public Optional<String> getValue(PolicyKey key) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(store.get(key));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Convenience helper — returns the value as an int.
     *
     * @throws IllegalStateException if the policy is not in cache
     * @throws NumberFormatException if stored value is not a valid integer
     */
    public int getInt(PolicyKey key) {
        return Integer.parseInt(require(key));
    }

    /**
     * Convenience helper — returns the value as a boolean.
     *
     * @throws IllegalStateException if the policy is not in cache
     */
    public boolean getBoolean(PolicyKey key) {
        return Boolean.parseBoolean(require(key));
    }

    private String require(PolicyKey key) {
        return getValue(key)
                .orElseThrow(() -> new IllegalStateException(
                        "Policy not found in cache: " + key + ". Has the DataSeeder run?"));
    }
}
