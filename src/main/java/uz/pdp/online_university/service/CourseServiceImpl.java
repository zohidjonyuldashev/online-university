package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.entity.Course;
import uz.pdp.online_university.enums.CourseStatus;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.CourseRepository;
import uz.pdp.online_university.security.AccessContext;
import uz.pdp.online_university.security.CurrentUser;
import uz.pdp.online_university.security.CustomUserDetails;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final AccessContext accessContext;

    // ---- Create ----

    @Override
    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        CurrentUser currentUser = getCurrentUser();

        // Determine owner: Admin can specify, Teacher is always self
        Long ownerTeacherId;
        if (currentUser.hasRole(RoleName.ADMIN) && request.getOwnerTeacherId() != null) {
            ownerTeacherId = request.getOwnerTeacherId();
        } else {
            ownerTeacherId = currentUser.getId();
        }

        Course course = Course.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .language(request.getLanguage().trim())
                .level(request.getLevel())
                .learningOutcomes(request.getLearningOutcomes())
                .prerequisites(request.getPrerequisites())
                .gradingPolicy(request.getGradingPolicy())
                .ownerTeacherId(ownerTeacherId)
                .programId(request.getProgramId())
                .build();

        course = courseRepository.save(course);

        log.info("Course created: '{}' (id={}) by user {} (owner={})",
                course.getTitle(), course.getId(), currentUser.getEmail(), ownerTeacherId);

        return toResponse(course);
    }

    // ---- Get All (filtered + scoped) ----

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CourseResponse> getAll(String search, CourseStatus status, Pageable pageable) {
        CurrentUser currentUser = getCurrentUser();

        Page<CourseResponse> page;

        // Student: only PUBLISHED + enrolled courses
        if (currentUser.hasRole(RoleName.STUDENT)) {
            Set<Long> enrolledCourseIds = accessContext.getAccessibleCourseIds();

            if (enrolledCourseIds == null || enrolledCourseIds.isEmpty()) {
                return PagedResponse.of(Page.empty(pageable));
            }

            page = courseRepository.findPublishedByIdInWithSearch(
                    enrolledCourseIds, search, pageable
            ).map(this::toResponse);

            return PagedResponse.of(page);
        }

        // Admin, AQAD, Deputy Director, Academic Dept: see all
        Set<Long> accessibleIds = accessContext.getAccessibleCourseIds();

        if (accessibleIds == null) {
            // Unrestricted — return all with filters
            page = courseRepository.findAllWithFilters(
                    search, status, null, pageable
            ).map(this::toResponse);
        } else if (accessibleIds.isEmpty()) {
            return PagedResponse.of(Page.empty(pageable));
        } else {
            // Teacher: only assigned courses
            page = courseRepository.findAllByIdInWithFilters(
                    accessibleIds, search, status, pageable
            ).map(this::toResponse);
        }

        return PagedResponse.of(page);
    }

    // ---- Get By ID ----

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        Course course = findCourseOrThrow(id);

        // Student can only see PUBLISHED courses
        CurrentUser currentUser = getCurrentUser();
        if (currentUser.hasRole(RoleName.STUDENT) && course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Course", "id", id);
        }

        return toResponse(course);
    }

    // ---- Update ----

    @Override
    @Transactional
    public CourseResponse update(Long id, UpdateCourseRequest request) {
        Course course = findCourseOrThrow(id);

        validateEditable(course);
        validateOwnership(course);
        validateVersion(course, request.getVersion());

        boolean majorChange = isMajorChange(course, request);

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getLanguage() != null) {
            course.setLanguage(request.getLanguage().trim());
        }
        if (request.getLevel() != null) {
            course.setLevel(request.getLevel());
        }
        if (request.getLearningOutcomes() != null) {
            course.setLearningOutcomes(request.getLearningOutcomes());
        }
        if (request.getPrerequisites() != null) {
            course.setPrerequisites(request.getPrerequisites());
        }
        if (request.getGradingPolicy() != null) {
            course.setGradingPolicy(request.getGradingPolicy());
        }

        if (majorChange && (course.getStatus() == CourseStatus.APPROVED
                || course.getStatus() == CourseStatus.PUBLISHED)) {
            course.setStatus(CourseStatus.REAPPROVAL_REQUIRED);
            log.info("Course '{}' (id={}) status changed to REAPPROVAL_REQUIRED due to major change",
                    course.getTitle(), course.getId());
        }

        course = courseRepository.save(course);

        log.info("Course updated: '{}' (id={}) by user {}",
                course.getTitle(), course.getId(), getCurrentUser().getEmail());

        return toResponse(course);
    }

    // ---- Submit for Review ----

    @Override
    @Transactional
    public CourseResponse submitForReview(Long id) {
        Course course = findCourseOrThrow(id);

        validateOwnership(course);
        validateTransition(course.getStatus(), CourseStatus.IN_REVIEW);

        course.setStatus(CourseStatus.IN_REVIEW);
        course = courseRepository.save(course);

        log.info("Course '{}' (id={}) submitted for review by {}",
                course.getTitle(), course.getId(), getCurrentUser().getEmail());

        return toResponse(course);
    }

    // ---- Publish ----

    @Override
    @Transactional
    public CourseResponse publish(Long id) {
        Course course = findCourseOrThrow(id);

        validateTransition(course.getStatus(), CourseStatus.PUBLISHED);

        course.setStatus(CourseStatus.PUBLISHED);
        course = courseRepository.save(course);

        log.info("Course '{}' (id={}) published by {}",
                course.getTitle(), course.getId(), getCurrentUser().getEmail());

        return toResponse(course);
    }

    // ---- Archive ----

    @Override
    @Transactional
    public CourseResponse archive(Long id) {
        Course course = findCourseOrThrow(id);

        validateOwnership(course);
        validateTransition(course.getStatus(), CourseStatus.ARCHIVED);

        course.setStatus(CourseStatus.ARCHIVED);
        course = courseRepository.save(course);

        log.info("Course '{}' (id={}) archived by {}",
                course.getTitle(), course.getId(), getCurrentUser().getEmail());

        return toResponse(course);
    }

    // =========================================================================
    // Validation Helpers
    // =========================================================================

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    private void validateOwnership(Course course) {
        CurrentUser currentUser = getCurrentUser();

        // Admin bypasses ownership check
        if (currentUser.hasRole(RoleName.ADMIN)) {
            return;
        }

        if (!course.getOwnerTeacherId().equals(currentUser.getId())) {
            throw new InvalidOperationException("You can only modify courses you own");
        }
    }

    private void validateEditable(Course course) {
        if (course.getStatus() == CourseStatus.IN_REVIEW) {
            throw new InvalidOperationException(
                    "Course cannot be edited while it is in review");
        }
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new InvalidOperationException(
                    "Archived course cannot be edited");
        }
    }

    private void validateVersion(Course course, Long requestVersion) {
        if (requestVersion != null && !requestVersion.equals(course.getVersion())) {
            throw new InvalidOperationException(
                    "Course was updated by another user. Please refresh and try again. "
                            + "Expected version: " + requestVersion + ", current version: " + course.getVersion());
        }
    }

    private void validateTransition(CourseStatus current, CourseStatus target) {
        boolean allowed = switch (current) {
            case DRAFT -> target == CourseStatus.IN_REVIEW
                    || target == CourseStatus.ARCHIVED;
            case IN_REVIEW -> target == CourseStatus.APPROVED
                    || target == CourseStatus.REJECTED;
            case APPROVED -> target == CourseStatus.PUBLISHED
                    || target == CourseStatus.ARCHIVED
                    || target == CourseStatus.REAPPROVAL_REQUIRED;
            case PUBLISHED -> target == CourseStatus.ARCHIVED
                    || target == CourseStatus.REAPPROVAL_REQUIRED;
            case REJECTED -> target == CourseStatus.DRAFT
                    || target == CourseStatus.ARCHIVED;
            case REAPPROVAL_REQUIRED -> target == CourseStatus.IN_REVIEW
                    || target == CourseStatus.ARCHIVED;
            case ARCHIVED -> false;
        };

        if (!allowed) {
            throw new InvalidOperationException(
                    "Invalid course status transition: " + current + " → " + target);
        }
    }

    private boolean isMajorChange(Course course, UpdateCourseRequest request) {
        return (request.getTitle() != null && !request.getTitle().equals(course.getTitle()))
                || (request.getDescription() != null && !request.getDescription().equals(course.getDescription()))
                || (request.getLanguage() != null && !request.getLanguage().equals(course.getLanguage()))
                || (request.getLearningOutcomes() != null && !request.getLearningOutcomes().equals(course.getLearningOutcomes()))
                || (request.getPrerequisites() != null && !request.getPrerequisites().equals(course.getPrerequisites()))
                || (request.getGradingPolicy() != null && !request.getGradingPolicy().equals(course.getGradingPolicy()));
    }

    // =========================================================================
    // Mapping + Security Helpers
    // =========================================================================

    private CurrentUser getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return new CurrentUser(userDetails);
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .language(course.getLanguage())
                .level(course.getLevel())
                .learningOutcomes(course.getLearningOutcomes())
                .prerequisites(course.getPrerequisites())
                .gradingPolicy(course.getGradingPolicy())
                .ownerTeacherId(course.getOwnerTeacherId())
                .programId(course.getProgramId())
                .status(course.getStatus())
                .version(course.getVersion())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
