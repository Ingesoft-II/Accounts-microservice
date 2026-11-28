package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.service.UpdateProfileService;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;

class UpdateProfileServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-28T00:00:00Z");

    private InMemoryAccountRepository cuentas;
    private UpdateProfileService servicio;
    private Account ana;

    @BeforeEach
    void setUp() {
        cuentas = new InMemoryAccountRepository();
        servicio = new UpdateProfileService(cuentas);
        ana = cuenta("ana.kp@gmail.com");
        cuentas.sembrar(ana);
    }

    @Test
    void updateCambiaElNombreYPersisteLaCuenta() {
        Cuenta actualizada = servicio.update(ana.id(), "Kouzou");

        assertThat(actualizada.displayName()).isEqualTo("Kouzou");
        assertThat(cuentas.buscarPorId(ana.id()).displayName()).isEqualTo("Kouzou");

        assertThat(actualizada.email()).isEqualTo("ana.kp@gmail.com");
        assertThat(actualizada.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void updateValidaElTamaNoDelNombreAlNivelDeDominio() {

        String nombreMuyLargo = "a".repeat(21);

        assertThatThrownBy(() -> servicio.update(ana.id(), nombreMuyLargo))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> servicio.update(ana.id(), "ab"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(cuentas.buscarPorId(ana.id()).displayName()).isEqualTo("anakp");
    }

    @Test
    void updateLanzaCuandoLaCuentaNoExiste() {
        assertThatThrownBy(() -> servicio.update(UUID.randomUUID(), "Kouzou"))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    private static Account cuenta(String email) {
        return new Account(UUID.randomUUID(), email, Account.nombreSaneado(email),
                Set.of(Role.PLAYER), AccountStatus.ACTIVE, null, "dev:" + email, null, AHORA);
    }
}
