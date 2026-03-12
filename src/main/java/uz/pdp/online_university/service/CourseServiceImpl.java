package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;
import uz.pdp.online_university.entity.Course;
import uz.pdp.online_university.enums.CourseStatus;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.CourseRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public CourseResponse create(CreateCourseRequest request) {
        Course course = Course.builder()
                .title(request.title())
                .description(request.description())
                .language(request.language())
                .level(request.level())
                .learningOutcomes(request.learningOutcomes())
                .prerequisites(request.prerequisites())
                .gradingPolicy(request.gradingPolicy())
                .ownerTeacherId(request.ownerTeacherId())
                .programId(request.programId())
                .status(CourseStatus.DRAFT)
                .build();

        courseRepository.save(course);
        return toResponse(course);
    }

    @Override
    public List<CourseResponse> getAll() {
        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CourseResponse getById(UUID id) {
        Course course = findCourse(id);
        return toResponse(course);
    }

    @Override
    public CourseResponse update(UUID id, UpdateCourseRequest request) {
        Course course = findCourse(id);

        validateEditable(course);

        if (request.version() != null && !request.version().equals(course.getVersion())) {
            throw new InvalidOperationException("Course was updated by another user. Please refresh and try again.");
        }

        boolean majorChange = isMajorChange(course, request);

        if (request.title() != null) {
            course.setTitle(request.title());
        }
        if (request.description() != null) {
            course.setDescription(request.description());
        }
        if (request.language() != null) {
            course.setLanguage(request.language());
        }
        if (request.level() != null) {
            course.setLevel(request.level());
        }
        if (request.learningOutcomes() != null) {
            course.setLearningOutcomes(request.learningOutcomes());
        }
        if (request.prerequisites() != null) {
            course.setPrerequisites(request.prerequisites());
        }
        if (request.gradingPolicy() != null) {
            course.setGradingPolicy(request.gradingPolicy());
        }

        if (majorChange &&
                (course.getStatus() == CourseStatus.APPROVED || course.getStatus() == CourseStatus.PUBLISHED)) {
            course.setStatus(CourseStatus.REAPPROVAL_REQUIRED);
        }

        courseRepository.save(course);
        return toResponse(course);
    }

    @Override
    public CourseResponse submitForReview(UUID id) {
        Course course = findCourse(id);

        validateTransition(course.getStatus(), CourseStatus.IN_REVIEW);
        course.setStatus(CourseStatus.IN_REVIEW);

        courseRepository.save(course);
        return toResponse(course);
    }

    @Override
    public CourseResponse publish(UUID id) {
        Course course = findCourse(id);

        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new InvalidOperationException("Course cannot be published unless it is APPROVED.");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);

        return toResponse(course);
    }

    @Override
    public CourseResponse archive(UUID id) {
        Course course = findCourse(id);

        validateTransition(course.getStatus(), CourseStatus.ARCHIVED);
        course.setStatus(CourseStatus.ARCHIVED);

        courseRepository.save(course);
        return toResponse(course);
    }

    private Course findCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private void validateEditable(Course course) {
        if (course.getStatus() == CourseStatus.IN_REVIEW) {
            throw new InvalidOperationException("Course cannot be edited while it is in review.");
        }

        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new InvalidOperationException("Archived course cannot be edited.");
        }
    }

    private void validateTransition(CourseStatus current, CourseStatus target) {
        boolean allowed = switch (current) {
            case DRAFT -> target == CourseStatus.IN_REVIEW || target == CourseStatus.ARCHIVED;
            case IN_REVIEW -> false;
            case APPROVED -> target == CourseStatus.PUBLISHED
                    || target == CourseStatus.ARCHIVED
                    || target == CourseStatus.REAPPROVAL_REQUIRED;
            case PUBLISHED -> target == CourseStatus.ARCHIVED
                    || target == CourseStatus.REAPPROVAL_REQUIRED;
            case REJECTED -> target == CourseStatus.DRAFT || target == CourseStatus.ARCHIVED;
            case REAPPROVAL_REQUIRED -> target == CourseStatus.IN_REVIEW || target == CourseStatus.ARCHIVED;
            case ARCHIVED -> false;
        };

        if (!allowed) {
            throw new InvalidOperationException(
                    "Invalid course status transition: " + current + " -> " + target
            );
        }
    }

    private boolean isMajorChange(Course course, UpdateCourseRequest request) {
        return (request.title() != null && !request.title().equals(course.getTitle()))
                || (request.description() != null && !request.description().equals(course.getDescription()))
                || (request.language() != null && !request.language().equals(course.getLanguage()))
                || (request.learningOutcomes() != null && !request.learningOutcomes().equals(course.getLearningOutcomes()))
                || (request.prerequisites() != null && !request.prerequisites().equals(course.getPrerequisites()))
                || (request.gradingPolicy() != null && !request.gradingPolicy().equals(course.getGradingPolicy()));
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLanguage(),
                course.getLevel(),
                course.getLearningOutcomes(),
                course.getPrerequisites(),
                course.getGradingPolicy(),
                course.getOwnerTeacherId(),
                course.getProgramId(),
                course.getStatus(),
                course.getVersion(),
                course.getUpdatedAt()
        );
    }
}
