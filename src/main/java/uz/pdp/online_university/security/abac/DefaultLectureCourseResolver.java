package uz.pdp.online_university.security.abac;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.exception.ResourceNotFoundException;

/**
 * Temporary implementation until Lecture entity/repository is built.
 * TODO: Will be replaced by the real implementation that queries LectureRepository.
 */
@Component
@ConditionalOnMissingBean(name = "lectureCourseResolverImpl")
public class DefaultLectureCourseResolver implements LectureCourseResolver {

    @Override
    public Long getCourseIdByLectureId(Long lectureId) {
        throw new ResourceNotFoundException("Lecture", "id", lectureId);
    }
}
