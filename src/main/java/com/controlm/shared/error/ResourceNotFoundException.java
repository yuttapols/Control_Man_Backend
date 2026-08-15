package com.controlm.shared.error;

/**
 * The resource does not exist, or exists but must stay hidden from this caller.
 *
 * <p>Both cases deliberately produce the same 404 so the response cannot be used to probe for
 * the existence of records the caller is not allowed to see.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
