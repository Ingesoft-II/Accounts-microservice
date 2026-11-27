package com.killerprotocol.cuentas.domain.exception;

import java.util.Objects;

public class DesafioInutilizableException extends RuntimeException {

    public enum Motivo {
        EXPIRADO,
        CONSUMIDO,
        AGOTADO
    }

    private final Motivo motivo;

    public DesafioInutilizableException(Motivo motivo, String detalle) {
        super("desafío inutilizable (" + motivo + "): " + detalle);
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    public Motivo motivo() {
        return motivo;
    }
}
