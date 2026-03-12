package uz.pdp.online_university.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.pdp.online_university.dto.response.DocumentResponse;
import uz.pdp.online_university.enums.DocumentType;
import uz.pdp.online_university.security.CustomUserDetails;
import uz.pdp.online_university.service.ApplicantDocumentService;

import java.util.List;

@RestController
@RequestMapping("/api/admission/documents")
@RequiredArgsConstructor
@Tag(name = "Admission Documents", description = "Endpoints for applicant document uploads")
public class AdmissionDocumentController {

    private final ApplicantDocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload admission document", description = "Uploads an admission document (PASSPORT, DIPLOMA, CERTIFICATE)")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type) {

        DocumentResponse response = documentService.uploadDocument(userDetails, file, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user documents", description = "Retrieves a list of documents uploaded by the authenticated user")
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<DocumentResponse> documents = documentService.getUserDocuments(principal.getId());
        return ResponseEntity.ok(documents);
    }
}
