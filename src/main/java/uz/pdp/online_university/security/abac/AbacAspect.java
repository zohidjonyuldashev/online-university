package uz.pdp.online_university.security.abac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.security.AccessContext;

import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AbacAspect {

    private final AccessContext accessContext;
    private final LectureCourseResolver lectureCourseResolver;

    @Before("@annotation(checkCourseAccess)")
    public void enforceCourseAccess(JoinPoint joinPoint, CheckCourseAccess checkCourseAccess) {
        Long courseId = extractParam(joinPoint, checkCourseAccess.paramName());

        if (!accessContext.canAccessCourse(courseId)) {
            throw new AccessDeniedException(
                    "You do not have access to this course");
        }
    }

    @Before("@annotation(checkLectureAccess)")
    public void enforceLectureAccess(JoinPoint joinPoint, CheckLectureAccess checkLectureAccess) {
        Long lectureId = extractParam(joinPoint, checkLectureAccess.paramName());

        Long courseId = lectureCourseResolver.getCourseIdByLectureId(lectureId);

        if (!accessContext.canAccessLecture(courseId)) {
            throw new AccessDeniedException(
                    "You do not have access to this lecture");
        }
    }

    @Before("@annotation(checkGroupAccess)")
    public void enforceGroupAccess(JoinPoint joinPoint, CheckGroupAccess checkGroupAccess) {
        Long groupId = extractParam(joinPoint, checkGroupAccess.paramName());

        if (!accessContext.canAccessGroup(groupId)) {
            throw new AccessDeniedException(
                    "You do not have access to this group");
        }
    }

    @Before("@annotation(checkEnrollmentAccess)")
    public void enforceEnrollmentAccess(JoinPoint joinPoint, CheckEnrollmentAccess checkEnrollmentAccess) {
        Long studentId = extractParam(joinPoint, checkEnrollmentAccess.paramName());

        if (!accessContext.canAccessEnrollment(studentId)) {
            throw new AccessDeniedException(
                    "You do not have access to this student's data");
        }
    }

    /**
     * Extracts a Long parameter value from the method arguments by parameter name.
     */
    private Long extractParam(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object value = args[i];

                if (value == null) {
                    throw new IllegalArgumentException(
                            "Parameter '" + paramName + "' must not be null");
                }

                if (value instanceof Long longValue) {
                    return longValue;
                }

                if (value instanceof String stringValue) {
                    try {
                        return Long.parseLong(stringValue);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Parameter '" + paramName + "' must be a valid number");
                    }
                }

                throw new IllegalArgumentException(
                        "Parameter '" + paramName + "' must be of type Long, got: "
                                + value.getClass().getSimpleName());
            }
        }

        throw new IllegalStateException(
                "Parameter '" + paramName + "' not found in method "
                        + signature.getDeclaringTypeName() + "." + signature.getName()
                        + ". Available parameters: " + String.join(", ",
                        java.util.Arrays.stream(parameters)
                                .map(Parameter::getName)
                                .toArray(String[]::new)));
    }
}
