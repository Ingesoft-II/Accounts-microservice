package com.killerprotocol.cuentas.application.service;

import java.time.Clock;
import java.util.Objects;

import com.killerprotocol.cuentas.application.port.in.LogoutUseCase;
import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Sha256;

public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepository tokens;
    private final Clock clock;

    public LogoutService(RefreshTokenRepository tokens, Clock clock) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void logout(String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken");
        RefreshToken token = tokens.buscarPorHash(Sha256.sha256Hex(refreshToken))
                .orElseThrow(RefrescoInvalidoException::new);
        token.revocar();
        tokens.guardar(token);
    }
}
