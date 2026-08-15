package com.controlm.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.controlm.shared.web.RequestIdHolder;
import java.time.Instant;

/**
 * Correlation metadata returned with every success response. {@code page} is present only on
 * list responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(String apiVersion, String requestId, Instant generatedAt, PageMeta page) {

    public static final String API_VERSION = "v1";

    public static ApiMeta current() {
        return current(null);
    }

    public static ApiMeta current(PageMeta page) {
        return new ApiMeta(API_VERSION, RequestIdHolder.currentRequestId(), Instant.now(), page);
    }
}
