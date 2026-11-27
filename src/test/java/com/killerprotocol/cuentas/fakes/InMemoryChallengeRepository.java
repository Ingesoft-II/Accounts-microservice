package com.killerprotocol.cuentas.fakes;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.killerprotocol.cuentas.application.port.out.ChallengeRepository;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;

public class InMemoryChallengeRepository implements ChallengeRepository {

    private final Map<UUID, TwoFactorChallenge> porId = new ConcurrentHashMap<>();
    private final Map<UUID, String> emailPorDesafio = new ConcurrentHashMap<>();

    @Override
    public void crear(TwoFactorChallenge desafio, String email) {
        porId.put(desafio.id(), desafio);
        emailPorDesafio.put(desafio.id(), email);
    }

    @Override
    public Optional<TwoFactorChallenge> buscarPorId(UUID id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public void guardar(TwoFactorChallenge desafio) {
        porId.put(desafio.id(), desafio);
    }

    @Override
    public void invalidarPendienteDe(Account cuenta) {
        for (TwoFactorChallenge desafio : porId.values()) {
            if (desafio.accountId().equals(cuenta.id()) && !desafio.estaConsumido()) {
                desafio.consumir();
            }
        }
    }

    @Override
    public long conteoRecientesPorEmail(String email, Instant desde) {

        return porId.values().stream()
                .filter(d -> {
                    String correo = emailPorDesafio.get(d.id());
                    return correo != null && correo.equalsIgnoreCase(email)
                            && !d.createdAt().isBefore(desde);
                })
                .count();
    }
}
