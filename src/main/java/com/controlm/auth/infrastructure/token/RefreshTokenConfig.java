package com.controlm.auth.infrastructure.token;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RefreshTokenProperties.class)
public class RefreshTokenConfig {}
