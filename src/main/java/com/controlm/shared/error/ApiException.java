package com.controlm.shared.error;

import java.util.List;

/**
 * Base type for failures that carry a client-facing {@link ErrorCode}.
 *
 * <p>The message of these exceptions is returned to the caller, so it must never contain SQL,
 * secrets, internal identifiers or anything that reveals whether a hidden resource exists.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldError> fieldErrors;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ErrorCode errorCode, String message, List<FieldError> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
