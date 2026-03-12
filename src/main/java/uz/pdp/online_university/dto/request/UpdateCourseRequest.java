package uz.pdp.online_university.dto.request;

public record UpdateCourseRequest(

        String title,
        String description,
        String language,
        String level,
        String learningOutcomes,
        String prerequisites,
        String gradingPolicy,
        Long version

) {
}
