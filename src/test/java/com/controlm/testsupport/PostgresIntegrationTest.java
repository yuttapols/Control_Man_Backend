package com.controlm.testsupport;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class PostgresIntegrationTest {
    private static final KeyPair JWT_KEYS = generateKeys();

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.issuer", () -> "https://issuer.control-m.test");
        registry.add("app.jwt.audience", () -> "control-m-portal-test");
        registry.add("app.jwt.private-key", () -> pem("PRIVATE KEY", JWT_KEYS.getPrivate().getEncoded()));
        registry.add("app.jwt.public-key", () -> pem("PUBLIC KEY", JWT_KEYS.getPublic().getEncoded()));
        registry.add("app.auth-web.allowed-origins", () -> "http://localhost:4200");
        registry.add("logging.structured.format.console", () -> "");
    }

    private static KeyPair generateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String pem(String type, byte[] value) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(value)
                + "\n-----END " + type + "-----";
    }
}
