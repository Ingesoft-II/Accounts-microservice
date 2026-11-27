package com.killerprotocol.cuentas.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;

public interface ChallengeRepository {

    void crear(TwoFactorChallenge desafio, String email);

    Optional<TwoFactorChallenge> buscarPorId(UUID id);

    void guardar(TwoFactorChallenge desafio);

    void invalidarPendienteDe(Account cuenta);

    long conteoRecientesPorEmail(String email, Instant desde);
}
