package com.killerprotocol.cuentas.fakes;

import java.util.ArrayList;
import java.util.List;

import com.killerprotocol.cuentas.application.event.JugadorRegistradoEvent;
import com.killerprotocol.cuentas.application.port.out.EventPublisher;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<JugadorRegistradoEvent> eventos = new ArrayList<>();

    @Override
    public void publicar(JugadorRegistradoEvent evento) {
        eventos.add(evento);
    }

    public List<JugadorRegistradoEvent> eventos() {
        return List.copyOf(eventos);
    }
}
