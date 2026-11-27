package com.killerprotocol.cuentas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;
import com.killerprotocol.cuentas.domain.model.Sha256;
import com.killerprotocol.cuentas.domain.model.TwoFactorChallenge;

/**
 * Pruebas de dominio de {@link TwoFactorChallenge}.
 */
class TwoFactorChallengeTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T19:10:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final UUID CUENTA = UUID.randomUUID();
    private static final String SALT = "salt-de-prueba";
    private static final String CODIGO = "123456";

    @Test
    void soloAceptaCodigosDeSeisDigitos() {
        assertThatIllegalArgumentException().isThrownBy(() -> TwoFactorChallenge.crear(CUENTA, "12345", SALT, Duration.ofMinutes(5), RELOJ));
        assertThatIllegalArgumentException().isThrownBy(() -> TwoFactorChallenge.crear(CUENTA, "1234567", SALT, Duration.ofMinutes(5), RELOJ));
        assertThatIllegalArgumentException().isThrownBy(() -> TwoFactorChallenge.crear(CUENTA, "12a456", SALT, Duration.ofMinutes(5), RELOJ));
        assertThatIllegalArgumentException().isThrownBy(() -> TwoFactorChallenge.crear(CUENTA, "abcdef", SALT, Duration.ofMinutes(5), RELOJ));
        assertThatIllegalArgumentException().isThrownBy(() -> TwoFactorChallenge.crear(CUENTA, null, SALT, Duration.ofMinutes(5), RELOJ));

        assertThat(TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ)).isNotNull();
    }

    @Test
    void elHashGuardadoEsSha256DeSaltMasCodigo() throws Exception {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] esperado = digest.digest((SALT + CODIGO).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : esperado) {
            hex.append(String.format("%02x", b));
        }

        assertThat(desafio.codeHash()).isEqualTo(hex.toString());
        assertThat(desafio.codeHash()).hasSize(64);
        assertThat(desafio.codeHash()).isNotEqualTo(CODIGO);
    }

    @Test
    void coincideCodigoSoloConElCodigoOriginal() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        assertThat(desafio.coincideCodigo(CODIGO)).isTrue();
        assertThat(desafio.coincideCodigo("000000")).isFalse();
        assertThat(desafio.coincideCodigo("12345")).isFalse();
        assertThat(desafio.coincideCodigo(null)).isFalse();
    }

    @Test
    void elHashGuardadoNoDependeDelOrdenSalMasCodigo() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        assertThat(desafio.codeHash()).isNotEqualTo(Sha256.sha256Hex(CODIGO + SALT));
        assertThat(desafio.codeHash()).isNotEqualTo(Sha256.sha256Hex(CODIGO));
    }

    @Test
    void expiraAPasadosLosCincoMinutos() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        assertThat(desafio.expiresAt()).isEqualTo(AHORA.plusSeconds(300));
        assertThat(desafio.estaExpirado(AHORA.plusSeconds(299))).isFalse();
        assertThat(desafio.estaExpirado(AHORA.plusSeconds(300))).isTrue();
        assertThat(desafio.estaExpirado(AHORA.plusSeconds(301))).isTrue();
    }

    @Test
    void elSextoIntentoLanzaPorqueSoloHayCincoPermitidos() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        for (int i = 0; i < TwoFactorChallenge.MAX_INTENTOS; i++) {
            desafio.registrarIntento(false);
        }
        assertThat(desafio.attempts()).isEqualTo(TwoFactorChallenge.MAX_INTENTOS);
        assertThat(desafio.estaAgotado()).isTrue();

        assertThatThrownBy(() -> desafio.registrarIntento(true))
                .isInstanceOf(DesafioInutilizableException.class)
                .extracting(e -> ((DesafioInutilizableException) e).motivo())
                .isEqualTo(DesafioInutilizableException.Motivo.AGOTADO);
    }

    @Test
    void unDesafioAgotadoQuedaInutilizableNiConElCodigoCorrecto() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        desafio.agoto();

        assertThat(desafio.estaInutilizable(AHORA)).isTrue();
        assertThatThrownBy(() -> desafio.registrarIntento(true))
                .isInstanceOf(DesafioInutilizableException.class);
    }

    @Test
    void unDesafioConsumidoQuedaInutilizable() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        desafio.consumir();

        assertThat(desafio.estaConsumido()).isTrue();
        assertThat(desafio.consumedAt()).isEqualTo(AHORA);
        assertThat(desafio.estaInutilizable(AHORA)).isTrue();
    }

    @Test
    void unDesafioExpiradoQuedaInutilizable() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        assertThat(desafio.estaInutilizable(AHORA)).isFalse();
        assertThat(desafio.estaInutilizable(AHORA.plusSeconds(301))).isTrue();
    }

    @Test
    void registraLosIntentosSinSuperarElMaximo() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        desafio.registrarIntento(false);
        desafio.registrarIntento(false);
        desafio.registrarIntento(false);

        assertThat(desafio.attempts()).isEqualTo(3);
        assertThat(desafio.estaAgotado()).isFalse();
        assertThat(desafio.estaInutilizable(AHORA)).isFalse();
    }

    @Test
    void exponeLaCuentaYLaCreacion() {
        TwoFactorChallenge desafio = TwoFactorChallenge.crear(CUENTA, CODIGO, SALT, Duration.ofMinutes(5), RELOJ);

        assertThat(desafio.accountId()).isEqualTo(CUENTA);
        assertThat(desafio.createdAt()).isEqualTo(AHORA);
        assertThat(desafio.salt()).isEqualTo(SALT);
        assertThat(desafio.id()).isNotNull();
    }
}
