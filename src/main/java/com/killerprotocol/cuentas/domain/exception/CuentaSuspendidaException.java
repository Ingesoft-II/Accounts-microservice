package com.killerprotocol.cuentas.domain.exception;

import java.time.Instant;
import java.util.Objects;

public class CuentaSuspendidaException extends RuntimeException {

    private final Instant until;

    public CuentaSuspendidaException(Instant until) {
        super("la cuenta está suspendida hasta " + until);
        this.until = Objects.requireNonNull(until, "until");
    }

    public Instant until() {
        return until;
    }
}
