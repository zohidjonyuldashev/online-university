package uz.pdp.online_university.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseRequest {

    private String title;

    private String description;

    private String language;

    private String level;

    private String learningOutcomes;

    private String prerequisites;

    private String gradingPolicy;

    // Required for optimistic lock validation
    private Long version;
}
