package com.killerprotocol.cuentas.application.port.in;

import java.util.UUID;

public interface UpdateProfileUseCase {

    Cuenta update(UUID playerId, String displayName);
}
