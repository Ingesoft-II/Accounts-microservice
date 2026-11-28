package com.killerprotocol.cuentas.application.port.in;

public interface RefreshSessionUseCase {

    Sesion refresh(String refreshToken);
}
