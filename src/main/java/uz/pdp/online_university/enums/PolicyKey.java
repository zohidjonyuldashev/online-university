package uz.pdp.online_university.enums;

/**
 * All supported policy keys in the system.
 * Each key maps to a row (or multiple versioned rows) in the policy_versions
 * table.
 */
public enum PolicyKey {

    /**
     * Minimum attendance percentage required to pass a course.
     * Value type: integer (0-100).
     * Default: 80
     */
    ATTENDANCE_THRESHOLD,

    /**
     * Minimum attendance % required for exam eligibility.
     * Value type: integer (0-100).
     * Default: 75
     */
    EXAM_ELIGIBILITY_MIN_ATTENDANCE,

    /**
     * Whether the student must have no outstanding debt to sit an exam.
     * Value type: boolean ("true" / "false").
     * Default: true
     */
    EXAM_ELIGIBILITY_REQUIRE_NO_DEBT,

    /**
     * Number of days student/course data is retained for GDPR compliance.
     * Value type: integer (days).
     * Default: 365
     */
    DATA_RETENTION_DAYS
}
