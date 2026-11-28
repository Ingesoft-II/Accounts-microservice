package com.killerprotocol.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.killerprotocol.cuentas.application.identity.Jugador;
import com.killerprotocol.cuentas.domain.exception.JugadorNoIdentificadoException;

class HeaderJugadorResolverTest {

    private final HeaderJugadorResolver resolver = new HeaderJugadorResolver();

    @Test
    void resuelveElIdDesdeElHeaderXPlayerId() {
        UUID id = UUID.randomUUID();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HeaderJugadorResolver.HEADER, id.toString());

        Jugador jugador = resolver.resolver(headers);

        assertThat(jugador.playerId()).isEqualTo(id);
    }

    @Test
    void lanzaCuandoFaltaElHeader() {
        assertThatThrownBy(() -> resolver.resolver(new HttpHeaders()))
                .isInstanceOf(JugadorNoIdentificadoException.class);
    }

    @Test
    void lanzaCuandoElHeaderEstaVacio() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HeaderJugadorResolver.HEADER, "   ");

        assertThatThrownBy(() -> resolver.resolver(headers))
                .isInstanceOf(JugadorNoIdentificadoException.class);
    }

    @Test
    void lanzaCuandoElHeaderNoEsUnUUID() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HeaderJugadorResolver.HEADER, "no-uuid");

        assertThatThrownBy(() -> resolver.resolver(headers))
                .isInstanceOf(JugadorNoIdentificadoException.class);
    }
}
