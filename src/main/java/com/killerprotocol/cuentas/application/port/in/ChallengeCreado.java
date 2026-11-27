package com.killerprotocol.cuentas.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record ChallengeCreado(UUID challengeId, String channel, int expiresInSec) {

    public ChallengeCreado {
        Objects.requireNonNull(challengeId, "challengeId");
        Objects.requireNonNull(channel, "channel");
        if (expiresInSec <= 0) {
            throw new IllegalArgumentException("expiresInSec debe ser positivo");
        }
    }
}
