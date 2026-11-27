package com.killerprotocol.cuentas.fakes;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Sha256;

public class FakeTokenIssuer implements TokenIssuer {

    private final Clock clock;

    public FakeTokenIssuer(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String emitirAccess(Account cuenta) {
        return "access-" + UUID.randomUUID();
    }

    @Override
    public RefreshEmitido nuevoRefresh(Account cuenta, UUID familyId, Duration ttl) {
        String valorOpaco = "refresh-" + UUID.randomUUID();
        RefreshToken token = new RefreshToken(UUID.randomUUID(), cuenta.id(),
                Sha256.sha256Hex(valorOpaco),
                familyId != null ? familyId : UUID.randomUUID(),
                clock.instant().plus(ttl), clock);
        return new RefreshEmitido(valorOpaco, token);
    }
}
