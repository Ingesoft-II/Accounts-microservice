package com.killerprotocol.cuentas.fakes;

import java.util.ArrayList;
import java.util.List;

import com.killerprotocol.cuentas.application.port.out.EmailSender;

public class FakeEmailSender implements EmailSender {

    public record Mensaje(String destinatario, String asunto, String cuerpo) {
    }

    private final List<Mensaje> mensajes = new ArrayList<>();

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        mensajes.add(new Mensaje(destinatario, asunto, cuerpo));
    }

    public List<Mensaje> mensajes() {
        return List.copyOf(mensajes);
    }

    public String ultimoCuerpo() {
        return mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1).cuerpo();
    }
}
