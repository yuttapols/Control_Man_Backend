package com.controlm.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LockoutPolicyTest {

    @Test
    @DisplayName("ยังไม่ล็อกเมื่อจำนวนครั้งที่ล้มยังไม่ถึงเพดาน")
    void belowThresholdDoesNotLock() {
        assertThat(LockoutPolicy.shouldLock(LockoutPolicy.MAX_FAILED_ATTEMPTS - 1)).isFalse();
    }

    @Test
    @DisplayName("ล็อกทันทีเมื่อจำนวนครั้งที่ล้มถึงเพดาน")
    void atThresholdLocks() {
        assertThat(LockoutPolicy.shouldLock(LockoutPolicy.MAX_FAILED_ATTEMPTS)).isTrue();
    }
}
