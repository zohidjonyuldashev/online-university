package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCourseRequest(

        @NotBlank
        String title,

        String description,

        @NotBlank
        String language,

        String level,

        String learningOutcomes,

        String prerequisites,

        String gradingPolicy,

        @NotNull
        UUID ownerTeacherId,

        UUID programId

) {
}
