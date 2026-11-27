package com.killerprotocol.cuentas.fakes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;
import com.killerprotocol.cuentas.domain.model.RefreshToken;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> porHash = new ConcurrentHashMap<>();

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return Optional.ofNullable(porHash.get(tokenHash));
    }

    @Override
    public void guardar(RefreshToken token) {
        porHash.put(token.tokenHash(), token);
    }

    @Override
    public void revocarFamilia(UUID familyId) {
        for (RefreshToken token : porHash.values()) {
            if (token.familyId().equals(familyId) && !token.esRevocado()) {
                token.revocar();
            }
        }
    }

    @Override
    public void revocarPorAccount(UUID accountId) {
        for (RefreshToken token : porHash.values()) {
            if (token.accountId().equals(accountId) && !token.esRevocado()) {
                token.revocar();
            }
        }
    }
}
