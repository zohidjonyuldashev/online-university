package uz.pdp.online_university.security.abac;

/**
 * Resolves the courseId for a given lectureId.
 * This is used by the ABAC aspect to check course-level access
 * when a lecture endpoint is called.
 */
public interface LectureCourseResolver {

    /**
     * @param lectureId the lecture ID
     * @return the courseId that this lecture belongs to
     * @throws uz.pdp.online_university.exception.ResourceNotFoundException if lecture not found
     */
    Long getCourseIdByLectureId(Long lectureId);
}
