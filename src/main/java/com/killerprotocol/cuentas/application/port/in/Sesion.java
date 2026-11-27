package com.killerprotocol.cuentas.application.port.in;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Role;

public record Sesion(String accessToken, String refreshToken, int expiresInSec,
                     UUID playerId, String displayName, Set<Role> roles) {

    public Sesion {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(displayName, "displayName");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        if (expiresInSec <= 0) {
            throw new IllegalArgumentException("expiresInSec debe ser positivo");
        }
    }
}
