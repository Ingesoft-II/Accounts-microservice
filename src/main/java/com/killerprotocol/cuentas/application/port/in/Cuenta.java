package com.killerprotocol.cuentas.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;

public record Cuenta(UUID playerId, String email, String displayName, Set<Role> roles,
                     AccountStatus status, Instant createdAt) {

    public Cuenta {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(displayName, "displayName");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Cuenta de(Account cuenta) {
        return new Cuenta(cuenta.id(), cuenta.email(), cuenta.displayName(), cuenta.roles(),
                cuenta.status(), cuenta.createdAt());
    }
}
