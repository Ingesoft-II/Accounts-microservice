package com.killerprotocol.cuentas.application.service;

import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.port.in.GetAccountUseCase;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.model.Account;

public class GetAccountService implements GetAccountUseCase {

    private final AccountRepository cuentas;

    public GetAccountService(AccountRepository cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
    }

    @Override
    public Cuenta me(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Account cuenta = cuentas.buscarPorId(playerId);
        if (cuenta == null) {
            throw new CuentaNoEncontradaException(playerId.toString());
        }
        return Cuenta.de(cuenta);
    }
}
