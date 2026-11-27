package com.killerprotocol.cuentas.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.RefreshToken;

public interface RefreshTokenRepository {

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    void guardar(RefreshToken token);

    void revocarFamilia(UUID familyId);

    void revocarPorAccount(UUID accountId);
}
