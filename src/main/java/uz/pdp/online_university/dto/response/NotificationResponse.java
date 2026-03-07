package uz.pdp.online_university.dto.response;

import lombok.Builder;
import lombok.Data;
import uz.pdp.online_university.entity.Notification;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationStatus;
import uz.pdp.online_university.enums.NotificationTemplateKey;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationTemplateKey templateKey;
    private NotificationChannel channel;
    private String subject;
    private String renderedBody;
    private NotificationStatus status;
    private Instant readAt;
    private Instant createdAt;
    private boolean unread;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .templateKey(n.getTemplateKey())
                .channel(n.getChannel())
                .subject(n.getSubject())
                .renderedBody(n.getRenderedBody())
                .status(n.getStatus())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .unread(n.getReadAt() == null)
                .build();
    }
}
