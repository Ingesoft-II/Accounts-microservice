package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.service.GetAccountService;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;

class GetAccountServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-28T00:00:00Z");

    private InMemoryAccountRepository cuentas;
    private GetAccountService servicio;

    @BeforeEach
    void setUp() {
        cuentas = new InMemoryAccountRepository();
        servicio = new GetAccountService(cuentas);
    }

    @Test
    void meDevuelveLaCuentaCompleta() {
        Account ana = cuenta("ana.kp@gmail.com", Set.of(Role.PLAYER, Role.MODERATOR));
        cuentas.sembrar(ana);

        Cuenta propia = servicio.me(ana.id());

        assertThat(propia.playerId()).isEqualTo(ana.id());
        assertThat(propia.email()).isEqualTo("ana.kp@gmail.com");
        assertThat(propia.displayName()).isEqualTo("anakp");
        assertThat(propia.roles()).containsExactlyInAnyOrder(Role.PLAYER, Role.MODERATOR);
        assertThat(propia.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(propia.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void meLanzaCuandoLaCuentaNoExiste() {
        UUID inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.me(inexistente))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    private static Account cuenta(String email, Set<Role> roles) {
        return new Account(UUID.randomUUID(), email, Account.nombreSaneado(email), roles,
                AccountStatus.ACTIVE, null, "dev:" + email, null, AHORA);
    }
}
