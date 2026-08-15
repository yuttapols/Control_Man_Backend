package com.controlm.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing for stored credentials.
 *
 * <p>A delegating encoder tags each hash with its algorithm (e.g. {@code {bcrypt}}), so the work
 * factor can be raised or the algorithm replaced later without invalidating existing hashes: old
 * hashes still verify while new ones use the current default.
 */
@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
