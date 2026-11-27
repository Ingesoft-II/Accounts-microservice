package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.port.in.ChallengeCreado;
import com.killerprotocol.cuentas.application.service.AuthenticateWithGoogleService;
import com.killerprotocol.cuentas.domain.exception.CuentaBaneadaException;
import com.killerprotocol.cuentas.domain.exception.CuentaSuspendidaException;
import com.killerprotocol.cuentas.domain.exception.LimiteDesafiosExcedidoException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.fakes.DevIdentityVerifier;
import com.killerprotocol.cuentas.fakes.FakeEmailSender;
import com.killerprotocol.cuentas.fakes.FakeTokenIssuer;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;
import com.killerprotocol.cuentas.fakes.InMemoryChallengeRepository;
import com.killerprotocol.cuentas.fakes.InMemoryEventPublisher;
import com.killerprotocol.cuentas.fakes.InMemoryRefreshTokenRepository;
import com.killerprotocol.cuentas.fakes.RelojMutable;

class AuthenticateWithGoogleFlowTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");
    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    private RelojMutable reloj;
    private InMemoryAccountRepository cuentas;
    private InMemoryChallengeRepository desafios;
    private FakeEmailSender correo;
    private AuthenticateWithGoogleService service;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(AHORA, ZoneOffset.UTC);
        cuentas = new InMemoryAccountRepository();
        desafios = new InMemoryChallengeRepository();
        correo = new FakeEmailSender();
        InMemoryEventPublisher eventos = new InMemoryEventPublisher();
        InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
        ReglasAuth reglas = new ReglasAuth("mod@kp.test", 300, 3, 600, 900, 7);
        service = new AuthenticateWithGoogleService(
                new DevIdentityVerifier(), cuentas, desafios, correo, reglas, reloj);
    }

    @Test
    void loginNuevoCreaLaCuentaYEnviaExactamenteUnCodigo() {
        ChallengeCreado creado = service.authenticate("dev:ana.kp@gmail.com");

        assertThat(creado.challengeId()).isNotNull();
        assertThat(creado.channel()).isEqualTo("EMAIL");
        assertThat(creado.expiresInSec()).isEqualTo(300);

        Account cuenta = cuentas.buscarPorGoogleSubject("dev:ana.kp@gmail.com");
        assertThat(cuenta).isNotNull();
        assertThat(cuenta.email()).isEqualTo("ana.kp@gmail.com");
        assertThat(cuenta.displayName()).isEqualTo("anakp");
        assertThat(cuenta.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(cuenta.roles()).containsExactly(Role.PLAYER);
        assertThat(cuenta.verifiedAt()).isNull();
        assertThat(cuenta.createdAt()).isEqualTo(AHORA);

        assertThat(correo.mensajes()).hasSize(1);
        assertThat(correo.mensajes().get(0).destinatario()).isEqualTo("ana.kp@gmail.com");
        assertThat(codigoDelCorreo()).isNotBlank();
    }

    @Test
    void cuentaBaneadaNoPuedeIniciarSesion() {
        sembrarCuenta("ana@kp.test", AccountStatus.BANNED, null);

        assertThatThrownBy(() -> service.authenticate("dev:ana@kp.test"))
                .isInstanceOf(CuentaBaneadaException.class);
        assertThat(correo.mensajes()).isEmpty();
    }

    @Test
    void cuentaSuspendidaActivaNoPuedeIniciarSesionYElErrorLlevaUntil() {
        Instant hasta = AHORA.plus(Duration.ofDays(1));
        sembrarCuenta("ana@kp.test", AccountStatus.SUSPENDED, hasta);

        assertThatThrownBy(() -> service.authenticate("dev:ana@kp.test"))
                .isInstanceOfSatisfying(CuentaSuspendidaException.class,
                        e -> assertThat(e.until()).isEqualTo(hasta));
        assertThat(correo.mensajes()).isEmpty();
    }

    @Test
    void cuentaSuspendidaVencidaPuedeIniciarSesionYPasaAActive() {
        Instant vencida = AHORA.minus(Duration.ofHours(1));
        sembrarCuenta("ana@kp.test", AccountStatus.SUSPENDED, vencida);

        ChallengeCreado creado = service.authenticate("dev:ana@kp.test");

        assertThat(creado.challengeId()).isNotNull();
        Account cuenta = cuentas.buscarPorGoogleSubject("dev:ana@kp.test");
        assertThat(cuenta.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(cuenta.suspendedUntil()).isNull();
        assertThat(correo.mensajes()).hasSize(1);
    }

    @Test
    void elSegundoLoginInvalidaElDesafioAnterior() {
        ChallengeCreado primero = service.authenticate("dev:ana@kp.test");
        ChallengeCreado segundo = service.authenticate("dev:ana@kp.test");

        assertThat(segundo.challengeId()).isNotEqualTo(primero.challengeId());

        var desafioAnterior = desafios.buscarPorId(primero.challengeId()).orElseThrow();
        var desafioNuevo = desafios.buscarPorId(segundo.challengeId()).orElseThrow();
        assertThat(desafioAnterior.estaConsumido()).isTrue();
        assertThat(desafioNuevo.estaConsumido()).isFalse();
        assertThat(desafioNuevo.estaExpirado(AHORA)).isFalse();
    }

    @Test
    void elCuartoDesafioEnDiezMinutosExcedeElLimite() {
        for (int i = 0; i < 3; i++) {
            service.authenticate("dev:ana@kp.test");
        }

        assertThatThrownBy(() -> service.authenticate("dev:ana@kp.test"))
                .isInstanceOf(LimiteDesafiosExcedidoException.class);
        assertThat(correo.mensajes()).hasSize(3);

        reloj.avanzar(Duration.ofSeconds(601));
        ChallengeCreado nuevo = service.authenticate("dev:ana@kp.test");
        assertThat(nuevo.challengeId()).isNotNull();
        assertThat(correo.mensajes()).hasSize(4);
    }

    @Test
    void elCorreoDeBootstrapRecibeElRolModeratorEnElAlta() {
        ChallengeCreado creado = service.authenticate("dev:mod@kp.test");
        assertThat(creado.challengeId()).isNotNull();

        Account cuenta = cuentas.buscarPorGoogleSubject("dev:mod@kp.test");
        assertThat(cuenta.roles()).containsExactlyInAnyOrder(Role.PLAYER, Role.MODERATOR);
    }

    private void sembrarCuenta(String email, AccountStatus estado, Instant suspendedUntil) {
        cuentas.guardar(new Account(java.util.UUID.randomUUID(), email, "ana",
                java.util.Set.of(Role.PLAYER), estado, suspendedUntil,
                "dev:" + email, AHORA.minus(Duration.ofDays(2)), AHORA.minus(Duration.ofDays(2))));
    }

    private String codigoDelCorreo() {
        Matcher matcher = CODIGO.matcher(correo.ultimoCuerpo());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
