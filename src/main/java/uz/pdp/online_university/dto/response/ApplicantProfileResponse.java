package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.ApplicantState;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicantProfileResponse {
    private Long id;
    private Long userId;
    private ApplicantState currentState;
    private LocalDateTime updatedAt;
}
