package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.pdp.online_university.audit.AuditLogService;
import uz.pdp.online_university.dto.response.DocumentResponse;
import uz.pdp.online_university.entity.ApplicantDocument;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.DocumentStatus;
import uz.pdp.online_university.enums.DocumentType;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.ApplicantDocumentRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.security.CustomUserDetails;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantDocumentService {

    private final ApplicantDocumentRepository repository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DocumentVirusScanner virusScanner;
    private final AuditLogService auditLogService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional
    public DocumentResponse uploadDocument(CustomUserDetails userDetails, MultipartFile file, DocumentType type) {

        Long id = userDetails.getId();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        validateFile(file);

        if (!virusScanner.isSafe(file)) {
            throw new IllegalArgumentException("File failed virus scan");
        }

        // Check if pending document exists, we might overwrite it
        Optional<ApplicantDocument> existingOpt = repository.findByUserIdAndType(user.getId(), type);
        if (existingOpt.isPresent()) {
            ApplicantDocument existing = existingOpt.get();
            if (existing.getStatus() == DocumentStatus.PENDING_REVIEW || existing.getStatus() == DocumentStatus.REJECTED) {
                // Delete old file
                storageService.delete(existing.getFilePath());
                repository.delete(existing);
            } else {
                throw new IllegalArgumentException("Cannot re-upload document that is already approved.");
            }
        }

        String storedFilename = storageService.store(file);

        ApplicantDocument document = new ApplicantDocument();
        document.setUser(user);
        document.setType(type);
        document.setFilePath(storedFilename);
        document.setStatus(DocumentStatus.PENDING_REVIEW);

        document = repository.save(document);

        // Record Audit log
        auditLogService.logAsync(
                user.getId(),
                "USER", // simplified actor role
                "ApplicantDocument",
                document.getId().toString(),
                AuditAction.CREATE,
                null,
                "Uploaded " + type + " document"
        );

        return DocumentResponse.builder()
                .id(document.getId())
                .type(document.getType())
                .status(document.getStatus())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    public List<DocumentResponse> getUserDocuments(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .type(doc.getType())
                        .status(doc.getStatus())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(
                contentType.equals("application/pdf") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Only PDF, JPG, and PNG files are allowed");
        }
    }
}
