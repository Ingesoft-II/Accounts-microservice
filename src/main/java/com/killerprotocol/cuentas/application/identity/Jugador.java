package com.killerprotocol.cuentas.application.identity;

import java.util.Objects;
import java.util.UUID;

public record Jugador(UUID playerId) {

    public Jugador {
        Objects.requireNonNull(playerId, "playerId");
    }
}
