package uz.pdp.online_university.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uz.pdp.online_university.audit.RequestContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RequestContext requestContext;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse response = baseError(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, WebRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorResponse response = baseError(
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, WebRequest request) {

        log.warn("Invalid operation: {}", ex.getMessage());

        ErrorResponse response = baseError(
                HttpStatus.BAD_REQUEST,
                "INVALID_OPERATION",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException ex, WebRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse response = baseError(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {

        ErrorResponse response = baseError(
                HttpStatus.UNAUTHORIZED,
                "BAD_CREDENTIALS",
                "Invalid email or password",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        ErrorResponse response = baseError(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You do not have permission to perform this action",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, WebRequest request) {

        ErrorResponse response = baseError(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "Account is deactivated. Contact administrator.",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, WebRequest request) {

        ErrorResponse response = baseError(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED",
                "Account is locked. Contact administrator.",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse response = baseError(
                HttpStatus.BAD_REQUEST,
                "ILLEGAL_ARGUMENT",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse response = baseError(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed",
                extractPath(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        String message = "Malformed request body";

        if (ex.getCause() instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {

            String fieldName = invalidFormat.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));

            String invalidValue = String.valueOf(invalidFormat.getValue());

            String allowedValues = Arrays.stream(invalidFormat.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            message = String.format(
                    "Invalid value '%s' for field '%s'. Allowed values: [%s]",
                    invalidValue, fieldName, allowedValues
            );
        }

        ErrorResponse response = baseError(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                message,
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String message;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            String allowedValues = Arrays.stream(ex.getRequiredType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            message = String.format(
                    "Invalid value '%s' for parameter '%s'. Allowed values: [%s]",
                    ex.getValue(), ex.getName(), allowedValues
            );
        } else {
            message = String.format(
                    "Invalid value '%s' for parameter '%s'",
                    ex.getValue(), ex.getName()
            );
        }

        ErrorResponse response = baseError(
                HttpStatus.BAD_REQUEST,
                "TYPE_MISMATCH",
                message,
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, WebRequest request) {

        log.warn("No resource found for path: {}", ex.getResourcePath());

        ErrorResponse response = baseError(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "Resource not found",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, WebRequest request) {

        log.error("Unexpected error: ", ex);

        ErrorResponse response = baseError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                extractPath(request),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    // ---- Optimistic lock exceptions ----

    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            jakarta.persistence.OptimisticLockException ex, WebRequest request) {

        log.warn("Optimistic lock conflict: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("This resource was modified by another user. Please refresh and try again.")
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLocking(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {

        log.warn("Optimistic lock conflict: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("This resource was modified by another user. Please refresh and try again.")
                .path(extractPath(request))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    private ErrorResponse baseError(HttpStatus status,
                                    String errorCode,
                                    String message,
                                    String path,
                                    Object details) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .correlationId(requestContext.getRequestId())
                .details(details)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
