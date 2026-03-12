package uz.pdp.online_university.dto.response;

import uz.pdp.online_university.enums.CourseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResponse(

        UUID id,
        String title,
        String description,
        String language,
        String level,
        String learningOutcomes,
        String prerequisites,
        String gradingPolicy,
        UUID ownerTeacherId,
        UUID programId,
        CourseStatus status,
        Long version,
        LocalDateTime updatedAt

) {
}
