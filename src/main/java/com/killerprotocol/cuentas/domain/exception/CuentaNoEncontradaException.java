package com.killerprotocol.cuentas.domain.exception;

public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(String playerId) {
        super("no existe la cuenta: " + playerId);
    }
}
