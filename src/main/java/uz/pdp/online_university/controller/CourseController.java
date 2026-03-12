package uz.pdp.online_university.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;
import uz.pdp.online_university.service.CourseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> create(@RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAll() {
        return ResponseEntity.ok(courseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateCourseRequest request) {

        return ResponseEntity.ok(courseService.update(id, request));
    }

    @PostMapping("/{id}/submit-review")
    public ResponseEntity<CourseResponse> submitReview(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.submitForReview(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.publish(id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<CourseResponse> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.archive(id));
    }
}
