package uz.pdp.online_university.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionCatalog {

    // User Management
    USER_CREATE("user.create", "user", "Create new users"),
    USER_READ("user.read", "user", "View user profiles"),
    USER_UPDATE("user.update", "user", "Update user profiles"),
    USER_DEACTIVATE("user.deactivate", "user", "Deactivate/suspend users"),
    USER_ROLE_ASSIGN("user.role.assign", "user", "Assign roles to users"),
    USER_SESSION_TERMINATE("user.session.terminate", "user", "Force logout users"),
    USER_PASSWORD_RESET("user.password.reset", "user", "Force password reset"),

    // Admission
    ADMISSION_APPLICATION_READ("admission.application.read", "admission", "View admission applications"),
    ADMISSION_APPLICATION_REVIEW("admission.application.review", "admission", "Review/verify documents"),
    ADMISSION_EXAM_SCHEDULE("admission.exam.schedule", "admission", "Schedule admission exams"),
    ADMISSION_DECISION_MAKE("admission.decision.make", "admission", "Make enrollment decisions"),

    // Academic
    ACADEMIC_PROGRAM_CREATE("academic.program.create", "academic", "Create academic programs"),
    ACADEMIC_PROGRAM_READ("academic.program.read", "academic", "View programs"),
    ACADEMIC_PROGRAM_UPDATE("academic.program.update", "academic", "Update programs"),
    ACADEMIC_GROUP_MANAGE("academic.group.manage", "academic", "Manage student groups"),
    ACADEMIC_SCHEDULE_MANAGE("academic.schedule.manage", "academic", "Manage timetable/schedule"),
    ACADEMIC_OVERRIDE_CREATE("academic.override.create", "academic", "Create access overrides for students"),
    ACADEMIC_ESCALATION_CREATE("academic.escalation.create", "academic", "Escalate issues to AQAD"),

    // Course
    COURSE_CREATE("course.create", "course", "Create courses"),
    COURSE_READ("course.read", "course", "View courses"),
    COURSE_UPDATE("course.update", "course", "Update courses"),
    COURSE_DELETE("course.delete", "course", "Delete draft courses"),
    COURSE_SUBMIT_REVIEW("course.submit.review", "course", "Submit course for AQAD review"),

    // Lecture
    LECTURE_CREATE("lecture.create", "lecture", "Create lectures"),
    LECTURE_READ("lecture.read", "lecture", "View lectures"),
    LECTURE_UPDATE("lecture.update", "lecture", "Update lectures"),
    LECTURE_DELETE("lecture.delete", "lecture", "Delete draft lectures"),
    LECTURE_CONDUCT("lecture.conduct", "lecture", "Start/conduct lectures"),

    // Material
    MATERIAL_UPLOAD("material.upload", "material", "Upload materials"),
    MATERIAL_READ("material.read", "material", "View/download materials"),
    MATERIAL_UPDATE("material.update", "material", "Update materials"),
    MATERIAL_DELETE("material.delete", "material", "Delete materials"),

    // Assessment
    ASSESSMENT_ASSIGNMENT_CREATE("assessment.assignment.create", "assessment", "Create assignments"),
    ASSESSMENT_ASSIGNMENT_READ("assessment.assignment.read", "assessment", "View assignments"),
    ASSESSMENT_SUBMISSION_CREATE("assessment.submission.create", "assessment", "Submit assignment answers"),
    ASSESSMENT_SUBMISSION_READ("assessment.submission.read", "assessment", "View submissions"),
    ASSESSMENT_GRADE_CREATE("assessment.grade.create", "assessment", "Grade submissions"),
    ASSESSMENT_GRADE_READ("assessment.grade.read", "assessment", "View grades"),
    ASSESSMENT_EXAM_SCHEDULE("assessment.exam.schedule", "assessment", "Schedule exams"),
    ASSESSMENT_EXAM_CONDUCT("assessment.exam.conduct", "assessment", "Conduct exams"),
    ASSESSMENT_APPEAL_CREATE("assessment.appeal.create", "assessment", "Submit appeal"),
    ASSESSMENT_APPEAL_REVIEW("assessment.appeal.review", "assessment", "Review appeals"),
    ASSESSMENT_RETAKE_MANAGE("assessment.retake.manage", "assessment", "Manage retake windows"),

    // Attendance
    ATTENDANCE_READ("attendance.read", "attendance", "View attendance records"),
    ATTENDANCE_MANAGE("attendance.manage", "attendance", "Manage attendance records"),

    // Communication
    COMMUNICATION_CHAT_SEND("communication.chat.send", "communication", "Send chat messages"),
    COMMUNICATION_CHAT_READ("communication.chat.read", "communication", "Read chat messages"),
    COMMUNICATION_QA_CREATE("communication.qa.create", "communication", "Create Q&A questions"),
    COMMUNICATION_QA_ANSWER("communication.qa.answer", "communication", "Answer Q&A questions"),
    COMMUNICATION_QA_MODERATE("communication.qa.moderate", "communication", "Moderate Q&A"),
    COMMUNICATION_COMMENT_CREATE("communication.comment.create", "communication", "Post comments"),
    COMMUNICATION_COMMENT_MODERATE("communication.comment.moderate", "communication", "Moderate comments"),

    // Finance
    FINANCE_CONTRACT_CREATE("finance.contract.create", "finance", "Create contracts"),
    FINANCE_CONTRACT_READ("finance.contract.read", "finance", "View contracts"),
    FINANCE_PAYMENT_READ("finance.payment.read", "finance", "View payments"),
    FINANCE_PAYMENT_SYNC("finance.payment.sync", "finance", "Sync payments"),
    FINANCE_BLOCK_MANAGE("finance.block.manage", "finance", "Manage financial blocks"),
    FINANCE_REPORT_READ("finance.report.read", "finance", "View financial reports"),

    // AQAD
    AQAD_REVIEW_CONDUCT("aqad.review.conduct", "aqad", "Conduct course reviews"),
    AQAD_REVIEW_DECIDE("aqad.review.decide", "aqad", "Approve/reject courses"),
    AQAD_AUDIT_CONDUCT("aqad.audit.conduct", "aqad", "Conduct audits"),
    AQAD_COMPLAINT_READ("aqad.complaint.read", "aqad", "View complaints"),
    AQAD_COMPLAINT_MANAGE("aqad.complaint.manage", "aqad", "Manage complaints"),
    AQAD_CORRECTIVE_CREATE("aqad.corrective.create", "aqad", "Create corrective actions"),
    AQAD_REPORT_READ("aqad.report.read", "aqad", "View AQAD reports"),

    // Resource
    RESOURCE_TEACHER_REGISTER("resource.teacher.register", "resource", "Register teachers"),
    RESOURCE_TEACHER_ASSIGN("resource.teacher.assign", "resource", "Assign teachers to courses"),
    RESOURCE_WORKLOAD_READ("resource.workload.read", "resource", "View workload reports"),
    RESOURCE_REPLACEMENT_MANAGE("resource.replacement.manage", "resource", "Manage teacher replacements"),

    // Dashboard
    DASHBOARD_KPI_READ("dashboard.kpi.read", "dashboard", "View KPI dashboards"),
    DASHBOARD_ANALYTICS_READ("dashboard.analytics.read", "dashboard", "View analytics"),
    DASHBOARD_REPORT_EXPORT("dashboard.report.export", "dashboard", "Export reports"),

    // System
    SYSTEM_CONFIG_MANAGE("system.config.manage", "system", "Manage system configuration"),
    SYSTEM_AUDIT_READ("system.audit.read", "system", "View audit logs"),
    SYSTEM_INTEGRATION_MANAGE("system.integration.manage", "system", "Manage integrations"),
    SYSTEM_MONITOR_READ("system.monitor.read", "system", "View system monitoring");

    private final String key;
    private final String module;
    private final String description;
}
