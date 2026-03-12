package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.CourseStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private String language;
    private String level;
    private String learningOutcomes;
    private String prerequisites;
    private String gradingPolicy;
    private Long ownerTeacherId;
    private Long programId;
    private CourseStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
