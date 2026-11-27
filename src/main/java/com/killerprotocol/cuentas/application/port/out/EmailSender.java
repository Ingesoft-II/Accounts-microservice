package com.killerprotocol.cuentas.application.port.out;

public interface EmailSender {

    void enviar(String destinatario, String asunto, String cuerpo);
}
