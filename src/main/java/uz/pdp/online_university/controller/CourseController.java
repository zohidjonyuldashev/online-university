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
import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.enums.CourseStatus;
import uz.pdp.online_university.security.abac.CheckCourseAccess;
import uz.pdp.online_university.service.CourseService;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAuthority('course.create')")
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CreateCourseRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('course.read')")
    public ResponseEntity<PagedResponse<CourseResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(courseService.getAll(search, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('course.read')")
    @CheckCourseAccess(paramName = "id")
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(courseService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "id")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request) {

        return ResponseEntity.ok(courseService.update(id, request));
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAuthority('course.submit.review')")
    @CheckCourseAccess(paramName = "id")
    public ResponseEntity<CourseResponse> submitForReview(@PathVariable Long id) {

        return ResponseEntity.ok(courseService.submitForReview(id));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @CheckCourseAccess(paramName = "id")
    public ResponseEntity<CourseResponse> publish(@PathVariable Long id) {

        return ResponseEntity.ok(courseService.publish(id));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('course.update')")
    @CheckCourseAccess(paramName = "id")
    public ResponseEntity<CourseResponse> archive(@PathVariable Long id) {

        return ResponseEntity.ok(courseService.archive(id));
    }
}
