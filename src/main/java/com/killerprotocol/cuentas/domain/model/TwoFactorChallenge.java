package com.killerprotocol.cuentas.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;

/**
 * Desafío de verificación 2FA por correo.
 */
public class TwoFactorChallenge {

    public static final int MAX_INTENTOS = 5;

    private final UUID id;
    private final UUID accountId;
    private final String codeHash;
    private final String salt;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Clock clock;

    private int attempts;
    private Instant consumedAt;

    private TwoFactorChallenge(UUID id, UUID accountId, String codeHash, String salt,
                               Instant createdAt, Instant expiresAt, int attempts,
                               Instant consumedAt, Clock clock) {
        this.id = id;
        this.accountId = accountId;
        this.codeHash = codeHash;
        this.salt = salt;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.consumedAt = consumedAt;
        this.clock = clock;
    }

    /**
     * Crea un desafío: valida que el código tenga exactamente 6 dígitos y guarda.
     */
    public static TwoFactorChallenge crear(UUID accountId, String codigo, String salt,
                                           Duration ttl, Clock clock) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(clock, "clock");
        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException("el código 2FA debe tener exactamente 6 dígitos");
        }
        Instant ahora = clock.instant();
        return new TwoFactorChallenge(UUID.randomUUID(), accountId,
                Sha256.sha256Hex(salt + codigo), salt, ahora, ahora.plus(ttl), 0, null, clock);
    }

    public void registrarIntento(boolean estaCorrecto) {
        if (estaAgotado()) {
            throw new DesafioInutilizableException(DesafioInutilizableException.Motivo.AGOTADO,
                    "se superaron los " + MAX_INTENTOS + " intentos permitidos");
        }
        attempts++;
    }

    public void agoto() {
        this.attempts = MAX_INTENTOS;
    }

    public void consumir() {
        this.consumedAt = clock.instant();
    }

    public boolean estaExpirado(Instant now) {
        Objects.requireNonNull(now, "now");
        return !expiresAt.isAfter(now);
    }

    public boolean estaConsumido() {
        return consumedAt != null;
    }

    public boolean estaAgotado() {
        return attempts >= MAX_INTENTOS;
    }

    public boolean estaInutilizable(Instant now) {
        return estaExpirado(now) || estaConsumido() || estaAgotado();
    }

    public boolean coincideCodigo(String codigo) {
        if (codigo == null || !codigo.matches("\\d{6}")) {
            return false;
        }
        return Sha256.sha256Hex(salt + codigo).equals(codeHash);
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public String codeHash() {
        return codeHash;
    }

    public String salt() {
        return salt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public int attempts() {
        return attempts;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
