package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @Size(max = 50, message = "Level must not exceed 50 characters")
    private String level;

    private String learningOutcomes;

    private String prerequisites;

    private String gradingPolicy;

    // Only Admin can specify this. For Teachers, it's ignored (auto-set to self).
    private Long ownerTeacherId;

    private Long programId;
}
