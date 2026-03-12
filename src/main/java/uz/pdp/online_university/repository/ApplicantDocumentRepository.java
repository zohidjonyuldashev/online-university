package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.online_university.entity.ApplicantDocument;
import uz.pdp.online_university.enums.DocumentType;

import java.util.List;
import java.util.Optional;

public interface ApplicantDocumentRepository extends JpaRepository<ApplicantDocument, Long> {
    List<ApplicantDocument> findByUserId(Long userId);
    Optional<ApplicantDocument> findByUserIdAndType(Long userId, DocumentType type);
}
