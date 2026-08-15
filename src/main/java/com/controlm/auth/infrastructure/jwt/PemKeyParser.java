package com.controlm.auth.infrastructure.jwt;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemKeyParser {
    private PemKeyParser() {}

    static RSAPrivateKey privateKey(String pem) {
        try {
            byte[] bytes = decode(pem, "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalArgumentException("JWT private key must be a PKCS#8 RSA PEM", ex);
        }
    }

    static RSAPublicKey publicKey(String pem) {
        try {
            byte[] bytes = decode(pem, "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalArgumentException("JWT public key must be an X.509 RSA PEM", ex);
        }
    }

    private static byte[] decode(String pem, String type) {
        if (pem == null || pem.isBlank()) throw new IllegalArgumentException("JWT " + type + " is required");
        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }
}
