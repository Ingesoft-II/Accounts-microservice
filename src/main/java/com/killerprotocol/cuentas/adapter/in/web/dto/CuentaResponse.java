package com.killerprotocol.cuentas.adapter.in.web.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.domain.model.Role;

public record CuentaResponse(String playerId, String email, String displayName,
                             List<String> roles, String status, Instant createdAt) {

    public static CuentaResponse de(Cuenta cuenta) {
        List<String> roles = cuenta.roles().stream()
                .sorted(Comparator.comparingInt(Role::ordinal))
                .map(Role::name)
                .toList();
        return new CuentaResponse(cuenta.playerId().toString(), cuenta.email(),
                cuenta.displayName(), roles, cuenta.status().name(), cuenta.createdAt());
    }
}
