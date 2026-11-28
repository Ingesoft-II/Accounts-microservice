package com.killerprotocol.cuentas.application.service;

import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.port.in.UpdateProfileUseCase;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.model.Account;

public class UpdateProfileService implements UpdateProfileUseCase {

    private final AccountRepository cuentas;

    public UpdateProfileService(AccountRepository cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
    }

    @Override
    public Cuenta update(UUID playerId, String displayName) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(displayName, "displayName");
        Account cuenta = cuentas.buscarPorId(playerId);
        if (cuenta == null) {
            throw new CuentaNoEncontradaException(playerId.toString());
        }

        cuenta.actualizarNombre(displayName);
        cuentas.guardar(cuenta);
        return Cuenta.de(cuenta);
    }
}
