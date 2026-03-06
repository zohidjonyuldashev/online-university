package uz.pdp.online_university.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Masks sensitive fields (password, token, secret, etc.) in serialized
 * snapshots
 * before they are written to the audit log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveFieldMasker {

    private static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "pin", "otp", "cvv", "ssn",
            "accessToken", "refreshToken", "apiKey", "privateKey", "passphrase");

    private final ObjectMapper objectMapper;

    /**
     * Converts any object to a JSON string with sensitive fields masked.
     *
     * @param obj the object to serialize (can be null)
     * @return JSON string with sensitive fields replaced by "***", or null if obj
     *         is null
     */
    @SuppressWarnings("unchecked")
    public String mask(Object obj) {
        if (obj == null)
            return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(obj, LinkedHashMap.class);
            maskRecursively(map);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize/mask object of type {}: {}", obj.getClass().getSimpleName(), e.getMessage());
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    /**
     * Masks a pre-built map (used when the snapshot is already a map).
     */
    public String maskMap(Map<String, Object> map) {
        if (map == null)
            return null;
        try {
            Map<String, Object> copy = new LinkedHashMap<>(map);
            maskRecursively(copy);
            return objectMapper.writeValueAsString(copy);
        } catch (Exception e) {
            log.warn("Failed to serialize masked map: {}", e.getMessage());
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private void maskRecursively(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSensitive(key)) {
                entry.setValue(MASK);
            } else if (value instanceof Map) {
                maskRecursively((Map<String, Object>) value);
            }
        }
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(s -> lower.contains(s.toLowerCase()));
    }
}
