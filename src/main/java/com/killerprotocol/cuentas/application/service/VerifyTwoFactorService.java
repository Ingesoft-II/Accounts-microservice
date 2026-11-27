package com.killerprotocol.cuentas.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.application.ReglasAuth;
import com.killerprotocol.cuentas.application.event.JugadorRegistradoEvent;
import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.port.in.VerifyTwoFactorUseCase;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.application.port.out.ChallengeRepository;
import com.killerprotocol.cuentas.application.port.out.EventPublisher;
import com.killerprotocol.cuentas.application.port.out.RefreshTokenRepository;
import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException.Motivo;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;

public class VerifyTwoFactorService implements VerifyTwoFactorUseCase {

    private final ChallengeRepository desafios;
    private final AccountRepository cuentas;
    private final RefreshTokenRepository refreshTokens;
    private final TokenIssuer tokens;
    private final EventPublisher eventos;
    private final ReglasAuth reglas;
    private final Clock clock;

    public VerifyTwoFactorService(ChallengeRepository desafios,
                                  AccountRepository cuentas,
                                  RefreshTokenRepository refreshTokens,
                                  TokenIssuer tokens,
                                  EventPublisher eventos,
                                  ReglasAuth reglas,
                                  Clock clock) {
        this.desafios = Objects.requireNonNull(desafios, "desafios");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Sesion verify(UUID challengeId, String codigo) {
        Objects.requireNonNull(challengeId, "challengeId");
        Instant ahora = clock.instant();

        TwoFactorChallenge desafio = desafios.buscarPorId(challengeId)
                .orElseThrow(() -> new DesafioInutilizableException(Motivo.EXPIRADO,
                        "no existe el desafío " + challengeId));

        if (desafio.estaAgotado()) {
            throw new DesafioInutilizableException(Motivo.AGOTADO,
                    "se superaron los intentos permitidos");
        }
        if (desafio.estaExpirado(ahora)) {
            throw new DesafioInutilizableException(Motivo.EXPIRADO,
                    "el desafío expiró en " + desafio.expiresAt());
        }
        if (desafio.estaConsumido()) {
            throw new DesafioInutilizableException(Motivo.CONSUMIDO,
                    "el desafío ya fue consumido");
        }

        boolean correcto = desafio.coincideCodigo(codigo);
        desafio.registrarIntento(correcto);
        desafios.guardar(desafio);
        if (!correcto) {

            if (desafio.estaAgotado()) {
                throw new DesafioInutilizableException(Motivo.AGOTADO,
                        "se superaron los intentos permitidos");
            }
            throw new DesafioInutilizableException(Motivo.EXPIRADO, "código incorrecto");
        }

        desafio.consumir();
        desafios.guardar(desafio);

        Account cuenta = cuentas.buscarPorId(desafio.accountId());
        if (cuenta == null) {
            throw new CuentaNoEncontradaException(desafio.accountId().toString());
        }

        String access = tokens.emitirAccess(cuenta);
        TokenIssuer.RefreshEmitido refresh = tokens.nuevoRefresh(cuenta, null,
                Duration.ofDays(reglas.refreshTtlDays()));
        refreshTokens.guardar(refresh.token());

        if (cuenta.marcarVerificada(ahora)) {
            cuentas.guardar(cuenta);
            eventos.publicar(new JugadorRegistradoEvent(UUID.randomUUID().toString(),
                    ahora, 1, cuenta.id().toString(), cuenta.displayName()));
        }

        return new Sesion(access, refresh.valorOpaco(), reglas.accessTtlSec(),
                cuenta.id(), cuenta.displayName(), cuenta.roles());
    }
}
