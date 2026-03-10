package uz.pdp.online_university.security.abac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckCourseAccess {

    /**
     * The name of the method parameter that holds the courseId.
     * Default is "courseId".
     */
    String paramName() default "courseId";
}
