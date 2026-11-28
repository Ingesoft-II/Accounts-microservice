package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.port.in.PerfilPublico;
import com.killerprotocol.cuentas.application.service.ListPublicProfilesService;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;

class ListPublicProfilesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-28T00:00:00Z");

    private InMemoryAccountRepository cuentas;
    private ListPublicProfilesService servicio;

    @BeforeEach
    void setUp() {
        cuentas = new InMemoryAccountRepository();
        servicio = new ListPublicProfilesService(cuentas);
    }

    @Test
    void exponeSoloPlayerIdYDisplayName() {
        Account a = sembrar("ana.kp@gmail.com");
        Account b = sembrar("bob.kp@gmail.com");

        List<PerfilPublico> perfiles = servicio.listar(Set.of(a.id(), b.id()));

        assertThat(perfiles)
                .extracting(PerfilPublico::playerId, PerfilPublico::displayName)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(a.id(), "anakp"),
                        org.assertj.core.groups.Tuple.tuple(b.id(), "bobkp"));
    }

    @Test
    void ignoraLosIdsInexistentes() {
        Account a = sembrar("ana.kp@gmail.com");

        List<PerfilPublico> perfiles = servicio.listar(Set.of(a.id(), UUID.randomUUID()));

        assertThat(perfiles).hasSize(1);
        assertThat(perfiles.get(0).playerId()).isEqualTo(a.id());
    }

    @Test
    void veinteIdsSonElMaximoPermitido() {
        Set<UUID> ids = new LinkedHashSet<>();
        for (int i = 0; i < 20; i++) {
            ids.add(siembraar("p" + i + "@kp.test"));
        }

        assertThat(servicio.listar(ids)).hasSize(20);
    }

    @Test
    void masDeVeinteIdsLanzaIllegalArgumentException() {
        Set<UUID> ids = new LinkedHashSet<>();
        for (int i = 0; i < 21; i++) {
            ids.add(UUID.randomUUID());
        }

        assertThatThrownBy(() -> servicio.listar(ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    private UUID siembraar(String email) {
        return sembrar(email).id();
    }

    private Account sembrar(String email) {
        Account cuenta = new Account(UUID.randomUUID(), email, Account.nombreSaneado(email),
                Set.of(Role.PLAYER), AccountStatus.ACTIVE, null, "dev:" + email, null, AHORA);
        cuentas.sembrar(cuenta);
        return cuenta;
    }
}
