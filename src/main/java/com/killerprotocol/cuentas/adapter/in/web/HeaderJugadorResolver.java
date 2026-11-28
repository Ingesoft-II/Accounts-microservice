package com.killerprotocol.cuentas.adapter.in.web;

import java.util.UUID;

import org.springframework.http.HttpHeaders;

import com.killerprotocol.cuentas.application.identity.Jugador;
import com.killerprotocol.cuentas.domain.exception.JugadorNoIdentificadoException;

public class HeaderJugadorResolver {

    public static final String HEADER = "X-Player-Id";

    public Jugador resolver(HttpHeaders headers) {
        String valor = firstHeader(headers, HEADER);
        if (valor == null || valor.isBlank()) {
            throw new JugadorNoIdentificadoException();
        }
        try {
            return new Jugador(UUID.fromString(valor.trim()));
        } catch (IllegalArgumentException e) {
            throw new JugadorNoIdentificadoException();
        }
    }

    private static String firstHeader(HttpHeaders headers, String nombre) {
        if (headers == null) {
            return null;
        }
        var valores = headers.get(nombre);
        return (valores == null || valores.isEmpty()) ? null : valores.get(0);
    }
}
