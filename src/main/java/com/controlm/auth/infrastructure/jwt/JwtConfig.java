package com.controlm.auth.infrastructure.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        RSAPublicKey publicKey = PemKeyParser.publicKey(properties.publicKey());
        RSAPrivateKey privateKey = PemKeyParser.privateKey(properties.privateKey());
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(PemKeyParser.publicKey(properties.publicKey())).build();
        var issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        var audience = new JwtClaimValidator<java.util.List<String>>("aud", values ->
                values != null && values.contains(properties.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }
}
