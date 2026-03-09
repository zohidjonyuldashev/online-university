package uz.pdp.online_university.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for notifications, keyed by
 * {@code userId}.
 *
 * <p>
 * Default: max {@code 10} notifications per user per {@code 60} seconds.
 * Both limits are configurable via application properties:
 * 
 * <pre>
 *   app.notification.rate-limit.max-per-window=10
 *   app.notification.rate-limit.window-seconds=60
 * </pre>
 */
@Slf4j
@Component
public class RateLimiter {

    private final int maxPerWindow;
    private final long windowSeconds;

    // userId → timestamps of recent sends within the window
    private final ConcurrentHashMap<Long, Deque<Instant>> windowMap = new ConcurrentHashMap<>();

    public RateLimiter(
            @Value("${app.notification.rate-limit.max-per-window:10}") int maxPerWindow,
            @Value("${app.notification.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxPerWindow = maxPerWindow;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Checks whether the given user is within the rate limit and, if so, records
     * this send.
     *
     * @param userId target user
     * @throws RateLimitExceededException if the limit is exceeded
     */
    public void checkAndRecord(Long userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> timestamps = windowMap.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict timestamps outside the window
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxPerWindow) {
                log.warn("Rate limit exceeded for userId={} ({} in {}s)", userId, maxPerWindow, windowSeconds);
                throw new RateLimitExceededException(
                        "Too many notifications. Max " + maxPerWindow + " per " + windowSeconds + " seconds.");
            }

            timestamps.addLast(now);
        }
    }

    // -------------------------------------------------------------------------
    // Visible for testing
    // -------------------------------------------------------------------------

    int getWindowSize(Long userId) {
        Deque<Instant> d = windowMap.get(userId);
        return d == null ? 0 : d.size();
    }

    void reset(Long userId) {
        windowMap.remove(userId);
    }
}
