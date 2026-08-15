package com.controlm.shared.error;

import com.controlm.shared.web.RequestIdHolder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns every failure into the Problem Details contract agreed with the frontend.
 *
 * <p>Two rules drive this class. A response must never leak a stack trace, SQL, schema detail
 * or secret, so unexpected exceptions are logged in full and answered with a fixed message.
 * And every response carries the request id, so a user reporting an error gives support
 * something to search for.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://errors.control-m/";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return respond(ex.errorCode(), ex.getMessage(), ex.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), codeOf(error.getCode()), error.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, "One or more fields are invalid", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, "One or more parameters are invalid", errors);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        // The caller's version is stale; they must reload before retrying.
        return respond(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "The record was changed by someone else. Reload and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        // The database message names constraints, tables and values, so it is logged, not returned.
        log.warn("Data integrity violation [requestId={}]", RequestIdHolder.currentRequestId(), ex);
        return respond(ErrorCode.DUPLICATE_RESOURCE, "The request conflicts with existing data");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.info("Authorization denied [requestId={}]", RequestIdHolder.currentRequestId());
        return respond(ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        // Deliberately uniform: the response must not reveal whether an account exists.
        return respond(ErrorCode.UNAUTHENTICATED, "Authentication required");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception [requestId={}]", RequestIdHolder.currentRequestId(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private static ResponseEntity<ProblemDetail> respond(ErrorCode code, String detail) {
        return respond(code, detail, List.of());
    }

    private static ResponseEntity<ProblemDetail> respond(ErrorCode code, String detail, List<FieldError> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.title());
        problem.setType(URI.create(TYPE_BASE + code.name().toLowerCase(Locale.ROOT)));
        problem.setProperty("code", code.name());
        problem.setProperty("requestId", RequestIdHolder.currentRequestId());
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.status(code.status()).body(problem);
    }

    private static FieldError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String field = path.substring(path.lastIndexOf('.') + 1);
        return new FieldError(field, codeOf(violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName()), violation.getMessage());
    }

    /** Bean Validation annotation names become stable screaming-snake codes for the frontend. */
    private static String codeOf(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return "INVALID";
        }
        return constraintName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}
