package com.killerprotocol.cuentas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.domain.model.RefreshToken;

/**
 * Pruebas de dominio de {@link RefreshToken}.
 */
class RefreshTokenTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T19:10:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final UUID CUENTA = UUID.randomUUID();
    private static final UUID FAMILIA = UUID.randomUUID();
    private static final Instant VIGENCIA_7_DIAS = AHORA.plus(Duration.ofDays(7));

    private static RefreshToken token(Instant expiraEn) {
        return new RefreshToken(UUID.randomUUID(), CUENTA, "hash-opaco", FAMILIA, expiraEn, RELOJ);
    }

    @Test
    void laRotacionMarcaElMomentoYLoDejaInactivo() {
        RefreshToken token = token(VIGENCIA_7_DIAS);

        assertThat(token.esRotado()).isFalse();
        assertThat(token.estaActivo(AHORA)).isTrue();

        token.rotar();

        assertThat(token.esRotado()).isTrue();
        assertThat(token.rotatedAt()).isEqualTo(AHORA);
        assertThat(token.estaActivo(AHORA)).isFalse();
    }

    @Test
    void laRevocacionMarcaElMomentoYLoDejaInactivo() {
        RefreshToken token = token(VIGENCIA_7_DIAS);

        token.revocar();

        assertThat(token.esRevocado()).isTrue();
        assertThat(token.revokedAt()).isEqualTo(AHORA);
        assertThat(token.estaActivo(AHORA)).isFalse();
    }

    @Test
    void expiraAPasadosLosSieteDias() {
        RefreshToken token = token(VIGENCIA_7_DIAS);

        assertThat(token.estaExpirado(VIGENCIA_7_DIAS.minus(Duration.ofHours(1)))).isFalse();
        assertThat(token.estaExpirado(VIGENCIA_7_DIAS)).isTrue();
        assertThat(token.estaExpirado(VIGENCIA_7_DIAS.plusSeconds(1))).isTrue();
    }

    @Test
    void unTokenExpiradoNoEstaActivo() {
        RefreshToken token = token(VIGENCIA_7_DIAS);

        assertThat(token.estaActivo(AHORA)).isTrue();
        assertThat(token.estaActivo(VIGENCIA_7_DIAS.plusSeconds(1))).isFalse();
    }

    @Test
    void losTokensDeUnaMismaFamiliaCompartenElFamilyId() {
        RefreshToken primero = token(VIGENCIA_7_DIAS);
        RefreshToken sucesor = new RefreshToken(UUID.randomUUID(), CUENTA, "hash-opaco-2",
                primero.familyId(), VIGENCIA_7_DIAS.plusSeconds(60), RELOJ);

        // El sucesor nace activo aunque su predecesor quede rotado (misma familia).
        primero.rotar();

        assertThat(sucesor.familyId()).isEqualTo(primero.familyId());
        assertThat(primero.estaActivo(AHORA)).isFalse();
        assertThat(sucesor.estaActivo(AHORA)).isTrue();
    }

    @Test
    void exigeTodosLosCamposDelToken() {
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RefreshToken(null, CUENTA, "h", FAMILIA, AHORA, RELOJ));
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RefreshToken(UUID.randomUUID(), null, "h", FAMILIA, AHORA, RELOJ));
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RefreshToken(UUID.randomUUID(), CUENTA, null, FAMILIA, AHORA, RELOJ));
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RefreshToken(UUID.randomUUID(), CUENTA, "h", null, AHORA, RELOJ));
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new RefreshToken(UUID.randomUUID(), CUENTA, "h", FAMILIA, null, RELOJ));
    }
}
