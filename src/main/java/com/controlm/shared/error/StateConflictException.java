package com.controlm.shared.error;

/**
 * The target is not in a state that allows the requested transition, for example approving a
 * revision that is no longer pending. Maps to 409.
 */
public class StateConflictException extends ApiException {

    public StateConflictException(String message) {
        super(ErrorCode.STATE_CONFLICT, message);
    }
}
