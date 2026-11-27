package com.killerprotocol.cuentas.fakes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public final class RelojMutable extends Clock {

    private final ZoneId zona;
    private Instant actual;

    public RelojMutable(Instant inicial, ZoneId zona) {
        this.actual = Objects.requireNonNull(inicial, "inicial");
        this.zona = Objects.requireNonNull(zona, "zona");
    }

    @Override
    public Instant instant() {
        return actual;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    public void avanzar(Duration duracion) {
        actual = actual.plus(duracion);
    }
}
