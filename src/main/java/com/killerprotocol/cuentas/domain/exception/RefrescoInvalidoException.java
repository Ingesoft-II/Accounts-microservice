package com.killerprotocol.cuentas.domain.exception;

public class RefrescoInvalidoException extends RuntimeException {

    public RefrescoInvalidoException() {
        super("el refresh token es inválido o está expirado");
    }
}
