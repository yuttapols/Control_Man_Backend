package com.controlm.auth.infrastructure.jwt;

import com.controlm.auth.application.AccessTokenService;
import com.controlm.iam.domain.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class JwtAccessTokenService implements AccessTokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JwtAccessTokenService(JwtEncoder encoder, JwtProperties properties) {
        this(encoder, properties, Clock.systemUTC());
    }

    JwtAccessTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String issue(AuthenticatedUser user, UUID sessionId, long authorizationVersion) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .audience(java.util.List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .claim("authVersion", authorizationVersion)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
