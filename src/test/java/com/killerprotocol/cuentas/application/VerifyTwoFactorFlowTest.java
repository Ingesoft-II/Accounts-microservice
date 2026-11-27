package com.killerprotocol.cuentas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.application.event.JugadorRegistradoEvent;
import com.killerprotocol.cuentas.application.port.in.ChallengeCreado;
import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.service.AuthenticateWithGoogleService;
import com.killerprotocol.cuentas.application.service.VerifyTwoFactorService;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;
import com.killerprotocol.cuentas.fakes.DevIdentityVerifier;
import com.killerprotocol.cuentas.fakes.FakeEmailSender;
import com.killerprotocol.cuentas.fakes.FakeTokenIssuer;
import com.killerprotocol.cuentas.fakes.InMemoryAccountRepository;
import com.killerprotocol.cuentas.fakes.InMemoryChallengeRepository;
import com.killerprotocol.cuentas.fakes.InMemoryEventPublisher;
import com.killerprotocol.cuentas.fakes.InMemoryRefreshTokenRepository;
import com.killerprotocol.cuentas.fakes.RelojMutable;

class VerifyTwoFactorFlowTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");
    private static final Pattern CODIGO = Pattern.compile("\\b(\\d{6})\\b");

    private RelojMutable reloj;
    private InMemoryAccountRepository cuentas;
    private InMemoryChallengeRepository desafios;
    private FakeEmailSender correo;
    private InMemoryEventPublisher eventos;
    private InMemoryRefreshTokenRepository refreshTokens;
    private AuthenticateWithGoogleService servicioGoogle;
    private VerifyTwoFactorService servicioVerify;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(AHORA, ZoneOffset.UTC);
        cuentas = new InMemoryAccountRepository();
        desafios = new InMemoryChallengeRepository();
        correo = new FakeEmailSender();
        eventos = new InMemoryEventPublisher();
        refreshTokens = new InMemoryRefreshTokenRepository();
        FakeTokenIssuer tokens = new FakeTokenIssuer(reloj);
        ReglasAuth reglas = new ReglasAuth("mod@kp.test", 300, 3, 600, 900, 7);
        servicioGoogle = new AuthenticateWithGoogleService(
                new DevIdentityVerifier(), cuentas, desafios, correo, reglas, reloj);
        servicioVerify = new VerifyTwoFactorService(
                desafios, cuentas, refreshTokens, tokens, eventos, reglas, reloj);
    }

    @Test
    void codigoCorrectoEmiteLaSesionFijaVerifiedAtYPublicaElEventoUnaSolaVez() {
        TwoFactorChallenge desafio = iniciarDesafio("ana.kp@gmail.com");
        String codigo = codigoDelCorreo();

        Sesion sesion = servicioVerify.verify(desafio.id(), codigo);

        assertThat(sesion.accessToken()).startsWith("access-");
        assertThat(sesion.refreshToken()).startsWith("refresh-");
        assertThat(sesion.expiresInSec()).isEqualTo(900);

        Account cuenta = cuentas.buscarPorGoogleSubject("dev:ana.kp@gmail.com");
        assertThat(sesion.playerId()).isEqualTo(cuenta.id());
        assertThat(sesion.displayName()).isEqualTo("anakp");
        assertThat(sesion.roles()).containsExactly(Role.PLAYER);
        assertThat(cuenta.verifiedAt()).isEqualTo(AHORA);
        assertThat(desafio.estaConsumido()).isTrue();

        assertThat(refreshTokens.buscarPorHash(
                com.killerprotocol.cuentas.domain.model.Sha256.sha256Hex(sesion.refreshToken())))
                .isPresent();

        assertThat(eventos.eventos()).hasSize(1);
        JugadorRegistradoEvent evento = eventos.eventos().get(0);
        assertThat(evento.playerId()).isEqualTo(cuenta.id().toString());
        assertThat(evento.displayName()).isEqualTo("anakp");
        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.occurredAt()).isEqualTo(AHORA);
        assertThat(evento.version()).isEqualTo(1);
    }

    @Test
    void loginRepetidoNoRepiteLaPublicacion() {
        verificarPrimerLogin("ana.kp@gmail.com");
        assertThat(eventos.eventos()).hasSize(1);

        TwoFactorChallenge segundo = iniciarDesafio("ana.kp@gmail.com");
        Sesion sesion = servicioVerify.verify(segundo.id(), codigoDelCorreo());

        assertThat(sesion.accessToken()).isNotBlank();
        assertThat(eventos.eventos()).hasSize(1);
    }

    @Test
    void cincoCodigosErradosAgotanElDesafioYDejaDeServirNiConElCodigoBueno() {
        TwoFactorChallenge desafio = iniciarDesafio("ana.kp@gmail.com");
        String codigoBueno = codigoDelCorreo();
        String codigoMalo = codigoBueno.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> servicioVerify.verify(desafio.id(), codigoMalo))
                    .isInstanceOf(DesafioInutilizableException.class);
            assertThat(desafio.estaAgotado()).isFalse();
        }

        assertThatThrownBy(() -> servicioVerify.verify(desafio.id(), codigoMalo))
                .isInstanceOfSatisfying(DesafioInutilizableException.class,
                        e -> assertThat(e.motivo()).isEqualTo(DesafioInutilizableException.Motivo.AGOTADO));
        assertThat(desafio.estaAgotado()).isTrue();
        assertThat(desafio.attempts()).isEqualTo(5);

        assertThatThrownBy(() -> servicioVerify.verify(desafio.id(), codigoBueno))
                .isInstanceOfSatisfying(DesafioInutilizableException.class,
                        e -> assertThat(e.motivo()).isEqualTo(DesafioInutilizableException.Motivo.AGOTADO));
        assertThat(eventos.eventos()).isEmpty();
        assertThat(cuentas.buscarPorGoogleSubject("dev:ana.kp@gmail.com").verifiedAt()).isNull();
    }

    @Test
    void desafioExpiradoNoSirveNiConElCodigoBueno() {
        TwoFactorChallenge desafio = iniciarDesafio("ana.kp@gmail.com");
        String codigo = codigoDelCorreo();
        reloj.avanzar(Duration.ofSeconds(301)); // TTL 2FA = 300 s

        assertThatThrownBy(() -> servicioVerify.verify(desafio.id(), codigo))
                .isInstanceOfSatisfying(DesafioInutilizableException.class,
                        e -> assertThat(e.motivo()).isEqualTo(DesafioInutilizableException.Motivo.EXPIRADO));
        assertThat(desafio.attempts()).isEqualTo(0);
        assertThat(eventos.eventos()).isEmpty();
    }

    @Test
    void desafioInexistenteNoSirve() {
        assertThatThrownBy(() -> servicioVerify.verify(UUID.randomUUID(), "123456"))
                .isInstanceOf(DesafioInutilizableException.class);
    }

    private void verificarPrimerLogin(String email) {
        TwoFactorChallenge desafio = iniciarDesafio(email);
        Sesion sesion = servicioVerify.verify(desafio.id(), codigoDelCorreo());
        assertThat(sesion.refreshToken()).isNotBlank();
    }

    private TwoFactorChallenge iniciarDesafio(String email) {
        ChallengeCreado creado = servicioGoogle.authenticate("dev:" + email);
        return desafios.buscarPorId(creado.challengeId()).orElseThrow();
    }

    private String codigoDelCorreo() {
        Matcher matcher = CODIGO.matcher(correo.ultimoCuerpo());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
