package com.killerprotocol.cuentas.application.port.in;

import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Account;

public record PerfilPublico(UUID playerId, String displayName) {

    public PerfilPublico {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(displayName, "displayName");
    }

    public static PerfilPublico de(Account cuenta) {
        return new PerfilPublico(cuenta.id(), cuenta.displayName());
    }
}
