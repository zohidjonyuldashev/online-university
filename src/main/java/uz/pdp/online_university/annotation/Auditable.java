package uz.pdp.online_university.annotation;

import uz.pdp.online_university.enums.AuditAction;

import java.lang.annotation.*;

/**
 * Marks a service method for automatic audit logging.
 *
 * <p>
 * Usage example:
 * 
 * <pre>
 * {@literal @}Auditable(entityType = "User", action = AuditAction.CREATE, entityIdExpression = "#result.id")
 * public User createUser(CreateUserRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** The logical entity type, e.g. "User", "Course", "Role". */
    String entityType();

    /** The audit action to record. */
    AuditAction action();

    /**
     * Optional SpEL expression to resolve the entity ID.
     * <ul>
     * <li>Use {@code #result.id} to extract from return value (for CREATE).</li>
     * <li>Use {@code #p0.id} for first parameter, {@code #p0} for a primitive ID
     * arg.</li>
     * </ul>
     */
    String entityIdExpression() default "#result?.id";

    /**
     * Optional SpEL expression pointing to the "before" object for UPDATE/DELETE.
     * When blank, the aspect will try to use the first argument.
     */
    String beforeExpression() default "";
}
