package com.killerprotocol.cuentas.application.port.out;

public interface GoogleIdentityVerifier {

    record Identidad(String subject, String email, String nombre) {
    }

    Identidad verify(String idToken);
}
