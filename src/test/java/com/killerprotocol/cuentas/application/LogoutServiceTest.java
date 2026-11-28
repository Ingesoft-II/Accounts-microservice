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
import com.killerprotocol.cuentas.application.service.LogoutService;
import com.killerprotocol.cuentas.application.service.RefreshSessionService;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.Sha256;
import com.killerprotocol.cuentas.fakes.FakeTokenIssuer;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;
import com.killerprotocol.cuentas.fakes.InMemoryRefreshTokenRepository;
import com.killerprotocol.cuentas.fakes.RelojMutable;

class LogoutServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");

    private RelojMutable reloj;
    private InMemoryAccountRepository cuentas;
    private InMemoryRefreshTokenRepository refreshTokens;
    private FakeTokenIssuer tokens;
    private LogoutService logout;
    private RefreshSessionService refresh;
    private Account cuenta;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(AHORA, ZoneOffset.UTC);
        cuentas = new InMemoryAccountRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        tokens = new FakeTokenIssuer(reloj);
        ReglasAuth reglas = new ReglasAuth("mod@kp.test", 300, 3, 600, 900, 7);
        logout = new LogoutService(refreshTokens, reloj);
        refresh = new RefreshSessionService(refreshTokens, cuentas, tokens, reglas, reloj);
        cuenta = cuenta("ana.kp@gmail.com");
    }

    @Test
    void logoutRevocaSoloElTokenPresentadoYNOLaFamilia() {
        String t0 = sembrarRefresh();
        Sesion s1 = refresh.refresh(t0);

        logout.logout(s1.refreshToken());

        assertThat(porHash(s1.refreshToken()).esRevocado()).isTrue();

        assertThat(porHash(t0).esRevocado()).isFalse();
        assertThat(porHash(t0).esRotado()).isTrue();
    }

    @Test
    void trasLogoutElTokenYaNoRefresca() {
        String t0 = sembrarRefresh();

        logout.logout(t0);

        assertThatThrownBy(() -> refresh.refresh(t0))
                .isInstanceOf(RefrescoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(t0));
    }

    @Test
    void logoutConTokenInexistenteFallas() {
        String inexistente = "refresh-" + UUID.randomUUID();

        assertThatThrownBy(() -> logout.logout(inexistente))
                .isInstanceOf(RefrescoInvalidoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(inexistente));
    }

    @Test
    void logoutNuloNoRefrescaNiLevantaFamilias() {
        String t0 = sembrarRefresh();

        assertThatThrownBy(() -> logout.logout(null))
                .isInstanceOf(NullPointerException.class);

        // el token sembrado sigue íntegro (nada fue revocado)
        assertThat(porHash(t0).esRevocado()).isFalse();
    }

    private String sembrarRefresh() {
        com.killerprotocol.cuentas.application.port.out.TokenIssuer.RefreshEmitido emitido =
                tokens.nuevoRefresh(cuenta, null, Duration.ofDays(7));
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
