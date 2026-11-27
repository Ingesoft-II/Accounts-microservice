package com.killerprotocol.cuentas.application.port.out;

import com.killerprotocol.cuentas.application.event.JugadorRegistradoEvent;

public interface EventPublisher {

    void publicar(JugadorRegistradoEvent evento);
}
