package com.killerprotocol.cuentas.domain.exception;

public class RefrescoReutilizadoException extends RuntimeException {

    public RefrescoReutilizadoException() {
        super("se reutilizó un refresh token ya rotado: la familia fue revocada");
    }
}
