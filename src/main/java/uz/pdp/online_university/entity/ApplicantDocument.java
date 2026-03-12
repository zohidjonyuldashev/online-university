package uz.pdp.online_university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uz.pdp.online_university.enums.DocumentStatus;
import uz.pdp.online_university.enums.DocumentType;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "applicant_documents")
public class ApplicantDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING_REVIEW;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}
