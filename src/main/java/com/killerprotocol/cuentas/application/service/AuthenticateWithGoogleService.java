package com.killerprotocol.cuentas.application.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.killerprotocol.cuentas.application.ReglasAuth;
import com.killerprotocol.cuentas.application.port.in.AuthenticateWithGoogleUseCase;
import com.killerprotocol.cuentas.application.port.in.ChallengeCreado;
import com.killerprotocol.cuentas.application.port.out.AccountRepository;
import com.killerprotocol.cuentas.application.port.out.ChallengeRepository;
import com.killerprotocol.cuentas.application.port.out.EmailSender;
import com.killerprotocol.cuentas.application.port.out.GoogleIdentityVerifier;
import com.killerprotocol.cuentas.domain.exception.CuentaBaneadaException;
import com.killerprotocol.cuentas.domain.exception.CuentaSuspendidaException;
import com.killerprotocol.cuentas.domain.exception.LimiteDesafiosExcedidoException;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;

public class AuthenticateWithGoogleService implements AuthenticateWithGoogleUseCase {

    static final String CANAL = "EMAIL";
    static final String ASUNTO = "Tu código de Killer Protocol";

    private final GoogleIdentityVerifier identidad;
    private final AccountRepository cuentas;
    private final ChallengeRepository desafios;
    private final EmailSender correo;
    private final ReglasAuth reglas;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public AuthenticateWithGoogleService(GoogleIdentityVerifier identidad,
                                         AccountRepository cuentas,
                                         ChallengeRepository desafios,
                                         EmailSender correo,
                                         ReglasAuth reglas,
                                         Clock clock) {
        this.identidad = Objects.requireNonNull(identidad, "identidad");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.desafios = Objects.requireNonNull(desafios, "desafios");
        this.correo = Objects.requireNonNull(correo, "correo");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ChallengeCreado authenticate(String idToken) {
        Objects.requireNonNull(idToken, "idToken");
        Instant ahora = clock.instant();
        GoogleIdentityVerifier.Identidad identidadVerificada = identidad.verify(idToken);

        Account cuenta = cuentas.buscarPorGoogleSubject(identidadVerificada.subject());
        if (cuenta == null) {

            Set<Role> roles = EnumSet.of(Role.PLAYER);
            if (reglas.esModeratorBootstrap(identidadVerificada.email())) {
                roles.add(Role.MODERATOR);
            }
            cuenta = new Account(UUID.randomUUID(), identidadVerificada.email(),
                    Account.nombreSaneado(identidadVerificada.email()),
                    roles, AccountStatus.ACTIVE, null, identidadVerificada.subject(), null, ahora);
            cuentas.guardar(cuenta);
        }

        if (cuenta.esBaneada()) {
            throw new CuentaBaneadaException();
        }

        if (cuenta.estaSuspendidaHasta(ahora)) {
            throw new CuentaSuspendidaException(cuenta.suspendedUntil());
        }
        if (cuenta.levantarSuspencionVencida(ahora)) {
            cuentas.guardar(cuenta);
        }
        if (reglas.esModeratorBootstrap(identidadVerificada.email())) {
            cuenta.otorgarRolModerator();
        }

        long recientes = desafios.conteoRecientesPorEmail(identidadVerificada.email(),
                ahora.minus(Duration.ofSeconds(reglas.ventanaSec())));
        if (recientes >= reglas.maxDesafiosPorVentana()) {
            throw new LimiteDesafiosExcedidoException();
        }

        desafios.invalidarPendienteDe(cuenta);
        String codigo = codigoAleatorio();
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(cuenta.id(), codigo,
                UUID.randomUUID().toString(), Duration.ofSeconds(reglas.twoFactorTtlSec()), clock);
        desafios.crear(desafio, identidadVerificada.email());

        correo.enviar(identidadVerificada.email(), ASUNTO, cuerpoDelCodigo(codigo));

        return new ChallengeCreado(desafio.id(), CANAL, reglas.twoFactorTtlSec());
    }

    private static String cuerpoDelCodigo(String codigo) {
        return "Tu código de verificación de Killer Protocol es: " + codigo
                + ". Válido durante 5 minutos. Si no lo solicitaste, ignora este correo.";
    }

    private String codigoAleatorio() {
        return String.format("%06d", aleatorio.nextInt(1_000_000));
    }
}
