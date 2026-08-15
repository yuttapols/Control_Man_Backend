package com.controlm.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenCodecTest {
    private final RefreshTokenCodec codec = new RefreshTokenCodec();

    @Test
    @DisplayName("refresh token ใช้ entropy 256-bit และเป็น URL-safe opaque value")
    void generatedTokensAreHighEntropyUrlSafeAndUnique() {
        String first = codec.generate();
        String second = codec.generate();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(second).hasSize(43).isNotEqualTo(first);
    }

    @Test
    @DisplayName("เก็บและค้น refresh token ด้วย SHA-256 hash ที่คงที่โดยไม่เหลือ token ดิบ")
    void hashingIsDeterministicAndDoesNotContainRawToken() {
        String raw = codec.generate();

        assertThat(codec.hash(raw))
                .startsWith("sha256:")
                .hasSize(71)
                .isEqualTo(codec.hash(raw))
                .doesNotContain(raw);
    }

    @Test
    @DisplayName("config ปฏิเสธ idle timeout ที่ยาวกว่า absolute TTL")
    void idleTimeoutCannotExceedTtl() {
        assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofHours(1), Duration.ofHours(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed");
    }
}
