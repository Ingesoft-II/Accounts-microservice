package com.killerprotocol.cuentas.application.port.in;

import java.util.UUID;

public interface GetAccountUseCase {

    Cuenta me(UUID playerId);
}
