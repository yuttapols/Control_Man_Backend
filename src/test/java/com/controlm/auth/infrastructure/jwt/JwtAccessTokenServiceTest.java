package com.controlm.auth.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.controlm.iam.domain.AuthenticatedUser;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtAccessTokenServiceTest {
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static KeyPair keys;

    @BeforeAll
    static void createKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
    }

    @Test
    @DisplayName("access token มี claims ขั้นต่ำครบและลงลายมือชื่อ RS256 ที่ตรวจสอบได้")
    void issuedTokenHasRequiredClaimsAndValidSignature() {
        JwtProperties properties = properties("portal-api", Duration.ofMinutes(15));
        JwtConfig config = new JwtConfig();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        var service = new JwtAccessTokenService(
                config.jwtEncoder(properties), properties, Clock.fixed(NOW, ZoneOffset.UTC));

        String token = service.issue(new AuthenticatedUser(userId, "alice", "Alice"), sessionId, 7);
        var jwt = config.jwtDecoder(properties).decode(token);

        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(jwt.getIssuer()).hasToString("https://issuer.control-m.test");
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getAudience()).containsExactly("portal-api");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(sessionId.toString());
        assertThat((Number) jwt.getClaim("authVersion")).hasToString("7");
        assertThat(jwt.getClaims()).doesNotContainKeys("username", "displayName", "email", "password");
    }

    @Test
    @DisplayName("token ที่ audience ไม่ตรงถูกปฏิเสธ")
    void wrongAudienceIsRejected() {
        JwtProperties issuedForPortal = properties("portal-api", Duration.ofMinutes(15));
        JwtProperties consumerDecoder = properties("consumer-api", Duration.ofMinutes(15));
        JwtConfig config = new JwtConfig();
        var service = new JwtAccessTokenService(
                config.jwtEncoder(issuedForPortal), issuedForPortal, Clock.systemUTC());
        String token = service.issue(
                new AuthenticatedUser(UUID.randomUUID(), "alice", "Alice"), UUID.randomUUID(), 1);

        JwtDecoder decoder = config.jwtDecoder(consumerDecoder);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("token ที่หมดอายุถูกปฏิเสธ")
    void expiredTokenIsRejected() {
        JwtProperties properties = properties("portal-api", Duration.ofMinutes(1));
        JwtConfig config = new JwtConfig();
        var service = new JwtAccessTokenService(
                config.jwtEncoder(properties),
                properties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC));
        String token = service.issue(
                new AuthenticatedUser(UUID.randomUUID(), "alice", "Alice"), UUID.randomUUID(), 1);

        assertThatThrownBy(() -> config.jwtDecoder(properties).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("config ปฏิเสธอายุ access token ที่ไม่เป็นบวก")
    void nonPositiveTtlIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("issuer", "audience", Duration.ZERO, "x", "y"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TTL");
    }

    private static JwtProperties properties(String audience, Duration ttl) {
        return new JwtProperties(
                "https://issuer.control-m.test",
                audience,
                ttl,
                pem("PRIVATE KEY", ((RSAPrivateKey) keys.getPrivate()).getEncoded()),
                pem("PUBLIC KEY", ((RSAPublicKey) keys.getPublic()).getEncoded()));
    }

    private static String pem(String type, byte[] bytes) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(bytes)
                + "\n-----END " + type + "-----";
    }
}
