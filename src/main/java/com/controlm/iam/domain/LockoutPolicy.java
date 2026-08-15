package com.controlm.iam.domain;

import java.time.Duration;

/**
 * Account lockout thresholds. After {@link #MAX_FAILED_ATTEMPTS} consecutive failed logins the
 * account is locked for {@link #LOCK_DURATION} to slow down online password guessing, without
 * telling the caller whether the account exists.
 */
public final class LockoutPolicy {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private LockoutPolicy() {
    }

    /** True once the running count of consecutive failures reaches the threshold. */
    public static boolean shouldLock(int consecutiveFailures) {
        return consecutiveFailures >= MAX_FAILED_ATTEMPTS;
    }
}
