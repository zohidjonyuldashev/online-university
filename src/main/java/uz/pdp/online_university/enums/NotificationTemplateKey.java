package uz.pdp.online_university.enums;

/**
 * Pre-defined notification template keys.
 * Each key maps to one or more {@link NotificationTemplate} rows
 * (one per channel).
 */
public enum NotificationTemplateKey {

    /** Admission application status changed (accepted / rejected / waitlisted). */
    ADMISSION_STATUS_UPDATE,

    /** An exam has been scheduled for the student's course. */
    EXAM_SCHEDULED,

    /** Exam results have been published. */
    EXAM_RESULT_PUBLISHED,

    /** Student's account has been blocked due to outstanding debt. */
    DEBT_BLOCKED,

    /** Student's debt block has been lifted. */
    DEBT_UNBLOCKED,

    /** AQAD committee has made a review decision. */
    AQAD_REVIEW_DECISION
}
