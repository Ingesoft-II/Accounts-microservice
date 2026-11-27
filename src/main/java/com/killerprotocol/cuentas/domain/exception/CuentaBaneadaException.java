package com.killerprotocol.cuentas.domain.exception;

public class CuentaBaneadaException extends RuntimeException {

    public CuentaBaneadaException() {
        super("la cuenta está baneada");
    }
}
