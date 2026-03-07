package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationTemplateKey;

import java.util.Map;

@Data
public class NotifyRequest {

    @NotNull(message = "templateKey must not be null")
    private NotificationTemplateKey templateKey;

    /** Defaults to IN_APP if not provided. */
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @NotNull(message = "userId must not be null")
    private Long userId;

    /** Variable name → value map for placeholder substitution. */
    private Map<String, String> variables = Map.of();
}
