package uz.pdp.online_university.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveFieldMaskerTest {

    private SensitiveFieldMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveFieldMasker(new ObjectMapper());
    }

    @Test
    @DisplayName("Should mask 'password' field in an object")
    void shouldMaskPasswordField() {
        var dto = Map.of(
                "email", "user@example.com",
                "password", "super-secret-123",
                "firstName", "John");

        String result = masker.maskMap((java.util.LinkedHashMap<String, Object>) null);
        assertThat(result).isNull();

        String json = masker.mask(dto);
        assertThat(json).contains("\"user@example.com\"");
        assertThat(json).contains("\"John\"");
        assertThat(json).contains("\"***\"");
        assertThat(json).doesNotContain("super-secret-123");
    }

    @Test
    @DisplayName("Should mask 'token' and 'secret' fields")
    void shouldMaskTokenAndSecretFields() {
        var dto = Map.of(
                "accessToken", "eyJhbGciOiJIUzI1NiJ9.abc.xyz",
                "refreshToken", "some-refresh-token",
                "apiKey", "ak_live_xyz",
                "username", "johndoe");

        String json = masker.mask(dto);
        assertThat(json).contains("\"***\"");
        assertThat(json).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(json).doesNotContain("some-refresh-token");
        assertThat(json).doesNotContain("ak_live_xyz");
        assertThat(json).contains("johndoe");
    }

    @Test
    @DisplayName("Should return null when input is null")
    void shouldReturnNullForNullInput() {
        assertThat(masker.mask(null)).isNull();
    }

    @Test
    @DisplayName("Non-sensitive fields should remain unchanged")
    void shouldNotMaskNonSensitiveFields() {
        var dto = Map.of(
                "id", 42,
                "email", "user@test.com",
                "firstName", "Jane");
        String json = masker.mask(dto);
        assertThat(json).contains("42");
        assertThat(json).contains("user@test.com");
        assertThat(json).contains("Jane");
        assertThat(json).doesNotContain("***");
    }
}
