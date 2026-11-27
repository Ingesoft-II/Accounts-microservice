package com.killerprotocol.cuentas.fakes;

import com.killerprotocol.cuentas.application.port.out.GoogleIdentityVerifier;

public class DevIdentityVerifier implements GoogleIdentityVerifier {

    @Override
    public Identidad verify(String idToken) {
        if (idToken == null || !idToken.startsWith("dev:")) {
            throw new IllegalArgumentException("id token inválido (se esperaba el formato dev:<email>)");
        }
        String email = idToken.substring("dev:".length());
        if (email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("id token inválido: el email no es válido");
        }
        return new Identidad("dev:" + email, email, null);
    }
}
