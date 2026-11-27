package com.killerprotocol.cuentas.application;

import java.util.Objects;

public record ReglasAuth(String moderatorEmail,
                         int twoFactorTtlSec,
                         int maxDesafiosPorVentana,
                         int ventanaSec,
                         int accessTtlSec,
                         int refreshTtlDays) {

    public ReglasAuth {
        if (twoFactorTtlSec <= 0) {
            throw new IllegalArgumentException("twoFactorTtlSec debe ser positivo");
        }
        if (maxDesafiosPorVentana <= 0) {
            throw new IllegalArgumentException("maxDesafiosPorVentana debe ser positivo");
        }
        if (ventanaSec <= 0) {
            throw new IllegalArgumentException("ventanaSec debe ser positiva");
        }
        if (accessTtlSec <= 0) {
            throw new IllegalArgumentException("accessTtlSec debe ser positivo");
        }
        if (refreshTtlDays <= 0) {
            throw new IllegalArgumentException("refreshTtlDays debe ser positivo");
        }
    }

    public boolean esModeratorBootstrap(String email) {
        return moderatorEmail != null && !moderatorEmail.isBlank()
                && moderatorEmail.equalsIgnoreCase(Objects.requireNonNull(email, "email"));
    }
}
