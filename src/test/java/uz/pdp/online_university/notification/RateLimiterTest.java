package uz.pdp.online_university.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // max 3 per 5-second window for test speed
        rateLimiter = new RateLimiter(3, 5);
    }

    @Test
    @DisplayName("Allows requests within the limit")
    void allowsWithinLimit() {
        assertThatNoException().isThrownBy(() -> {
            rateLimiter.checkAndRecord(1L);
            rateLimiter.checkAndRecord(1L);
            rateLimiter.checkAndRecord(1L);
        });
    }

    @Test
    @DisplayName("Throws RateLimitExceededException when limit is exceeded")
    void throwsWhenLimitExceeded() {
        rateLimiter.checkAndRecord(2L);
        rateLimiter.checkAndRecord(2L);
        rateLimiter.checkAndRecord(2L);

        assertThatThrownBy(() -> rateLimiter.checkAndRecord(2L))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Too many notifications");
    }

    @Test
    @DisplayName("Limits are per-user — different users don't interfere")
    void limitsArePerUser() {
        rateLimiter.checkAndRecord(10L);
        rateLimiter.checkAndRecord(10L);
        rateLimiter.checkAndRecord(10L);

        // user 11 should be unaffected
        assertThatNoException().isThrownBy(() -> rateLimiter.checkAndRecord(11L));
    }

    @Test
    @DisplayName("Window size is correctly tracked")
    void windowSizeTracked() {
        rateLimiter.checkAndRecord(5L);
        rateLimiter.checkAndRecord(5L);
        assertThat(rateLimiter.getWindowSize(5L)).isEqualTo(2);
    }

    @Test
    @DisplayName("Reset clears window for user")
    void resetClearsWindow() {
        rateLimiter.checkAndRecord(6L);
        rateLimiter.checkAndRecord(6L);
        rateLimiter.reset(6L);
        assertThat(rateLimiter.getWindowSize(6L)).isEqualTo(0);
    }
}
