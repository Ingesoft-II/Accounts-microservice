package com.killerprotocol.cuentas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cuentas")
public record CuentasProperties(Auth auth, Jwt jwt, Bootstrap bootstrap) {

    public record Auth(Google google, TwoFactor twoFactor, Refresh refresh) {

        public record Google(String mode) {
        }

        public record TwoFactor(int ttlSec, int maxAttempts, int maxChallengesPerWindow, int windowSec) {
        }

        public record Refresh(int ttlDays) {
        }
    }

    public record Jwt(String keyPath, String issuer, String audience, int ttlSec) {
    }

    public record Bootstrap(String moderatorEmail) {
    }
}
