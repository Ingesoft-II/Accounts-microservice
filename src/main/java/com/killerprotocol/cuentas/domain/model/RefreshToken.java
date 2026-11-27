package com.killerprotocol.cuentas.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token opaco, rotativo y organizado en familias.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID accountId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant expiresAt;
    private final Clock clock;

    private Instant rotatedAt;
    private Instant revokedAt;

    public RefreshToken(UUID id, UUID accountId, String tokenHash, UUID familyId,
                        Instant expiresAt, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void rotar() {
        this.rotatedAt = clock.instant();
    }

    public void revocar() {
        this.revokedAt = clock.instant();
    }

    public boolean esRotado() {
        return rotatedAt != null;
    }

    public boolean esRevocado() {
        return revokedAt != null;
    }

    public boolean estaExpirado(Instant now) {
        Objects.requireNonNull(now, "now");
        return !expiresAt.isAfter(now);
    }

    public boolean estaActivo(Instant now) {
        Objects.requireNonNull(now, "now");
        return !esRotado() && !esRevocado() && !estaExpirado(now);
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant rotatedAt() {
        return rotatedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
