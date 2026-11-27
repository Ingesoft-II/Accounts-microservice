package com.killerprotocol.cuentas.application.port.out;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.RefreshToken;

public interface TokenIssuer {

    record RefreshEmitido(String valorOpaco, RefreshToken token) {

        public RefreshEmitido {
            Objects.requireNonNull(valorOpaco, "valorOpaco");
            Objects.requireNonNull(token, "token");
        }
    }

    String emitirAccess(Account cuenta);

    RefreshEmitido nuevoRefresh(Account cuenta, UUID familyId, Duration ttl);
}
