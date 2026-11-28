package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.service.RefreshSessionService;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.exception.RefrescoReutilizadoException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.Sha256;
import com.killerprotocol.cuentas.fakes.FakeTokenIssuer;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;
import com.killerprotocol.cuentas.fakes.InMemoryRefreshTokenRepository;
import com.killerprotocol.cuentas.fakes.RelojMutable;

class RefreshServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");

    private RelojMutable reloj;
    private InMemoryAccountRepository cuentas;
    private InMemoryRefreshTokenRepository refreshTokens;
    private FakeTokenIssuer tokens;
    private RefreshSessionService servicio;
    private Account cuenta;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(AHORA, ZoneOffset.UTC);
        cuentas = new InMemoryAccountRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        tokens = new FakeTokenIssuer(reloj);
        servicio = new RefreshSessionService(refreshTokens, cuentas, tokens,
                new ReglasAuth("mod@kp.test", 300, 3, 600, 900, 7), reloj);
        cuenta = cuenta("ana.kp@gmail.com");
    }

    @Test
    void rotacionEncadenadaFuncionaYConservaLaMismaFamilia() {
        String t0 = sembrarRefresh(null);
        String hash0 = Sha256.sha256Hex(t0);

        Sesion s1 = servicio.refresh(t0);
        assertThat(s1.accessToken()).startsWith("access-");
        assertThat(s1.refreshToken()).isNotEqualTo(t0);
        assertThat(s1.expiresInSec()).isEqualTo(900);
        assertThat(s1.playerId()).isEqualTo(cuenta.id());
        assertThat(s1.displayName()).isEqualTo(cuenta.displayName());
        assertThat(s1.roles()).containsExactly(Role.PLAYER);

        assertThat(porHash(t0).esRotado()).isTrue();

        Sesion s2 = servicio.refresh(s1.refreshToken());
        RefreshToken tok1 = porHash(s1.refreshToken());
        RefreshToken tok2 = porHash(s2.refreshToken());

        assertThat(tok2.familyId()).isEqualTo(tok1.familyId());
        assertThat(tok1.tokenHash()).isNotEqualTo(hash0);

        assertThat(tok2.expiresAt()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
        assertThat(s2.expiresInSec()).isEqualTo(900);
    }

    @Test
    void reutilizarUnTokenRotadoRevocaLaFamiliaYElHijoDejaDeServir() {
        String t0 = sembrarRefresh(null);
        Sesion s1 = servicio.refresh(t0);

        assertThatThrownBy(() -> servicio.refresh(t0))
                .isInstanceOf(RefrescoReutilizadoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(t0));

        RefreshToken hijo = porHash(s1.refreshToken());
        assertThat(hijo.esRevocado()).isTrue();
        assertThatThrownBy(() -> servicio.refresh(s1.refreshToken()))
                .isInstanceOf(RefrescoInvalidoException.class);
    }

    @Test
    void tokenExpiradoNoRefrescaYNoSeRota() {
        String t0 = sembrarRefresh(null);
        reloj.avanzar(Duration.ofDays(7).plusSeconds(1));

        assertThatThrownBy(() -> servicio.refresh(t0))
                .isInstanceOf(RefrescoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(t0));
        assertThat(porHash(t0).esRotado()).isFalse();
    }

    @Test
    void tokenInexistenteNoRefresca() {
        String inexistente = "refresh-" + UUID.randomUUID();

        assertThatThrownBy(() -> servicio.refresh(inexistente))
                .isInstanceOf(RefrescoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(inexistente));
    }

    @Test
    void tokenRevolcadoPorFueraTampocoRefresca() {
        String t0 = sembrarRefresh(null);
        refreshTokens.revocarFamilia(porHash(t0).familyId());

        assertThatThrownBy(() -> servicio.refresh(t0))
                .isInstanceOf(RefrescoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(t0));
    }

    private String sembrarRefresh(UUID familyId) {
        com.killerprotocol.cuentas.application.port.out.TokenIssuer.RefreshEmitido emitido =
                tokens.nuevoRefresh(cuenta, familyId, Duration.ofDays(7));
        refreshTokens.guardar(emitido.token());
        return emitido.valorOpaco();
    }

    private RefreshToken porHash(String opaco) {
        return refreshTokens.buscarPorHash(Sha256.sha256Hex(opaco)).orElseThrow();
    }

    private Account cuenta(String email) {
        Account c = new Account(UUID.randomUUID(), email, email.split("@")[0],
                Set.of(Role.PLAYER), AccountStatus.ACTIVE, null, "dev:" + email,
                AHORA, AHORA);
        cuentas.sembrar(c);
        return c;
    }
}
