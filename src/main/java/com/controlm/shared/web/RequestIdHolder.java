package com.controlm.shared.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * Holds the correlation id of the request being served, so responses, logs and audit records
 * can be tied together.
 *
 * <p>The value is kept in the SLF4J {@link MDC} so structured logging picks it up without
 * every log statement having to pass it explicitly.
 */
public final class RequestIdHolder {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /**
     * A caller-supplied id is only trusted when it looks like an id. Anything else is replaced,
     * so a client cannot inject newlines or control characters into the log stream.
     */
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_.:-]{1,64}$");

    private RequestIdHolder() {}

    /** Returns the id for the current request, or {@code null} outside a request. */
    public static String currentRequestId() {
        return MDC.get(MDC_KEY);
    }

    static String acceptOrGenerate(String candidate) {
        if (candidate != null && SAFE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return generate();
    }

    static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    static void set(String requestId) {
        MDC.put(MDC_KEY, requestId);
    }

    static void clear() {
        MDC.remove(MDC_KEY);
    }
}
