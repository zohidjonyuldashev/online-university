package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationTemplateKey;

/**
 * Reusable notification template stored in the database.
 * Body supports {{variable}} placeholders rendered by
 * {@link uz.pdp.online_university.notification.TemplateRenderer}.
 *
 * Table: notification_templates
 */
@Entity
@Table(name = "notification_templates", uniqueConstraints = @UniqueConstraint(name = "uq_template_key_channel", columnNames = {
        "template_key", "channel" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "template_key", nullable = false, length = 60)
    private NotificationTemplateKey templateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    /**
     * Email subject or in-app title. Supports {{variable}} placeholders.
     */
    @Column(name = "subject", nullable = false, length = 300)
    private String subject;

    /**
     * Full body / HTML for email, plain text for in-app.
     * Supports {{variable}} placeholders.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * JSON array of expected variable names, e.g. ["examName","date"].
     * Used for validation at send-time.
     */
    @Column(name = "variables_json", columnDefinition = "TEXT")
    private String variablesJson;

    /**
     * Inactive templates are skipped at send-time (soft disable).
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
