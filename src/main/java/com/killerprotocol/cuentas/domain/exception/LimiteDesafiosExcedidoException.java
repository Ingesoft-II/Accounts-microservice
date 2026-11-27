package com.killerprotocol.cuentas.domain.exception;

public class LimiteDesafiosExcedidoException extends RuntimeException {

    public LimiteDesafiosExcedidoException() {
        super("se superó el límite de desafíos por correo en la última hora de ventana");
    }
}
