package com.killerprotocol.cuentas.domain.exception;

public class JugadorNoIdentificadoException extends RuntimeException {

    public JugadorNoIdentificadoException() {
        super("falta el header X-Player-Id");
    }
}
