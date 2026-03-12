package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.DocumentStatus;
import uz.pdp.online_university.enums.DocumentType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponse {
    private Long id;
    private DocumentType type;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;
}
