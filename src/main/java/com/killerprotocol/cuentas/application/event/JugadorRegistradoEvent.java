package com.killerprotocol.cuentas.application.event;

import java.time.Instant;
import java.util.Objects;

public record JugadorRegistradoEvent(String eventId, Instant occurredAt, int version,
                                     String playerId, String displayName) {

    public JugadorRegistradoEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(displayName, "displayName");
    }
}
