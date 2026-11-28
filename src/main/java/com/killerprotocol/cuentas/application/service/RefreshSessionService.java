package com.killerprotocol.cuentas.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.killerprotocol.cuentas.application.ReglasAuth;
import com.killerprotocol.cuentas.application.port.in.RefreshSessionUseCase;
import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;
import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Sha256;

/**
 * TODO
 */
public class RefreshSessionService implements RefreshSessionUseCase {

    private final RefreshTokenRepository tokens;
    private final AccountRepository cuentas;
    private final TokenIssuer issuer;
    private final ReglasAuth reglas;
    private final Clock clock;

    public RefreshSessionService(RefreshTokenRepository tokens, AccountRepository cuentas,
                                 TokenIssuer issuer, ReglasAuth reglas, Clock clock) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Sesion refresh(String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken");
        Instant ahora = clock.instant();
        RefreshToken token = tokens.buscarPorHash(Sha256.sha256Hex(refreshToken))
                .orElseThrow(RefrescoInvalidoException::new);
        if (token.estaExpirado(ahora) || token.esRevocado()) {
            throw new RefrescoInvalidoException();
        }
        // TODO:
        token.rotar();
        tokens.guardar(token);

        Account cuenta = cuentas.buscarPorId(token.accountId());
        if (cuenta == null) {
            throw new CuentaNoEncontradaException(token.accountId().toString());
        }

        String access = issuer.emitirAccess(cuenta);
        TokenIssuer.RefreshEmitido nuevo = issuer.nuevoRefresh(cuenta, token.familyId(),
                Duration.ofDays(reglas.refreshTtlDays()));
        tokens.guardar(nuevo.token());

        return new Sesion(access, nuevo.valorOpaco(), reglas.accessTtlSec(),
                cuenta.id(), cuenta.displayName(), cuenta.roles());
    }
}
