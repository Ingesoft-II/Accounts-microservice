package com.killerprotocol.cuentas.application.port.in;

import java.util.UUID;

public interface VerifyTwoFactorUseCase {

    Sesion verify(UUID challengeId, String codigo);
}
