package com.controlm.shared.error;

/**
 * The request was syntactically valid but breaks a business rule (a {@code BR-*} rule from the
 * design documents). Maps to 422.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
