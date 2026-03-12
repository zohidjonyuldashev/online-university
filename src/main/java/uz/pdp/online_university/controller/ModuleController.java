package uz.pdp.online_university.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.request.CreateModuleRequest;
import uz.pdp.online_university.dto.request.ReorderModulesRequest;
import uz.pdp.online_university.dto.request.UpdateModuleRequest;
import uz.pdp.online_university.dto.response.ModuleResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.security.abac.CheckCourseAccess;
import uz.pdp.online_university.service.ModuleService;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<ModuleResponse> create(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateModuleRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleService.create(courseId, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('course.read')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<List<ModuleResponse>> getAll(@PathVariable Long courseId) {

        return ResponseEntity.ok(moduleService.getAllByCourseId(courseId));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('course.read')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<PagedResponse<ModuleResponse>> getAllPaged(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderIndex") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(moduleService.getAllByCourseIdPaged(courseId, pageable));
    }

    @GetMapping("/{moduleId}")
    @PreAuthorize("hasAuthority('course.read')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<ModuleResponse> getById(
            @PathVariable Long courseId,
            @PathVariable Long moduleId) {

        return ResponseEntity.ok(moduleService.getById(courseId, moduleId));
    }

    @PatchMapping("/{moduleId}")
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<ModuleResponse> update(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateModuleRequest request) {

        return ResponseEntity.ok(moduleService.update(courseId, moduleId, request));
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<Void> delete(
            @PathVariable Long courseId,
            @PathVariable Long moduleId) {

        moduleService.delete(courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "courseId")
    public ResponseEntity<PagedResponse<ModuleResponse>> reorder(
            @PathVariable Long courseId,
            @Valid @RequestBody ReorderModulesRequest request) {

        return ResponseEntity.ok(moduleService.reorder(courseId, request));
    }
}
