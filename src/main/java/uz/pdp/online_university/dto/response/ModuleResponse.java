package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.ModuleStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private ModuleStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}