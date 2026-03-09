package uz.pdp.online_university.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.entity.NotificationTemplate;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationTemplateKey;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.NotificationTemplateRepository;

import java.util.List;

/**
 * Manages {@link NotificationTemplate} records.
 * Only active templates are returned for send-time lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    /**
     * Finds an active template for the given key and channel.
     *
     * @throws ResourceNotFoundException if no active template is found
     */
    @Transactional(readOnly = true)
    public NotificationTemplate findActive(NotificationTemplateKey key, NotificationChannel channel) {
        return templateRepository.findByTemplateKeyAndChannelAndActiveTrue(key, channel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NotificationTemplate", "key+channel", key + "+" + channel));
    }

    /**
     * Returns all currently active templates (admin view).
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplate> findAllActive() {
        return templateRepository.findAllByActiveTrue();
    }

    /**
     * Persist a template (used by
     * {@link uz.pdp.online_university.config.DataSeeder}).
     */
    @Transactional
    public NotificationTemplate save(NotificationTemplate template) {
        return templateRepository.save(template);
    }
}
