package com.killerprotocol.cuentas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;

/**
 * Pruebas de dominio de {@link Account}.
 */
class AccountTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T19:10:00Z");

    private static Account cuenta(AccountStatus status, Instant suspendedUntil) {
        return new Account(UUID.randomUUID(), "ana@kp.test", "ana",
                Set.of(Role.PLAYER), status, suspendedUntil, "dev:ana@kp.test", null, AHORA);
    }

    @Test
    void unaSuspensionActivaBloqueaElLogin() {
        Account cuenta = cuenta(AccountStatus.SUSPENDED, AHORA.plusSeconds(3600));

        assertThat(cuenta.estaSuspendidaHasta(AHORA)).isTrue();
    }

    @Test
    void unaSuspensionVencidaPasaAActivaYLimpiasuspendedUntil() {
        Instant vencimiento = AHORA.minusSeconds(60);
        Account cuenta = cuenta(AccountStatus.SUSPENDED, vencimiento);

        boolean cambio = cuenta.levantarSuspencionVencida(AHORA);

        assertThat(cambio).isTrue();
        assertThat(cuenta.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(cuenta.suspendedUntil()).isNull();
        assertThat(cuenta.estaSuspendidaHasta(AHORA)).isFalse();
    }

    @Test
    void unaSuspensionActivaNoSeLevantaAntesDeSuVencimiento() {
        Account cuenta = cuenta(AccountStatus.SUSPENDED, AHORA.plusSeconds(3600));

        assertThat(cuenta.levantarSuspencionVencida(AHORA)).isFalse();
        assertThat(cuenta.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(cuenta.estaSuspendidaHasta(AHORA)).isTrue();
    }

    @Test
    void unaSuspensionExactamenteEnElVencimientoSeLevanta() {
        Instant vencimiento = AHORA;
        Account cuenta = cuenta(AccountStatus.SUSPENDED, vencimiento);

        assertThat(cuenta.levantarSuspencionVencida(AHORA)).isTrue();
        assertThat(cuenta.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(cuenta.suspendedUntil()).isNull();
    }

    @Test
    void elBanEsPermanenteYNoSeLevanta() {
        Account cuenta = cuenta(AccountStatus.BANNED, null);
        cuenta.aplicarBan();

        assertThat(cuenta.esBaneada()).isTrue();
        assertThat(cuenta.status()).isEqualTo(AccountStatus.BANNED);
        assertThat(cuenta.levantarSuspencionVencida(AHORA.plusSeconds(999_999))).isFalse();
        cuenta.aplicarSuspencion(AHORA.plusSeconds(3600));
        assertThat(cuenta.status()).isEqualTo(AccountStatus.BANNED);
        assertThat(cuenta.esBaneada()).isTrue();
    }

    @Test
    void aplicarSuspencionFijaElEstadoYElHasta() {
        Account cuenta = cuenta(AccountStatus.ACTIVE, null);
        Instant hasta = AHORA.plusSeconds(86_400);

        cuenta.aplicarSuspencion(hasta);

        assertThat(cuenta.status()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(cuenta.suspendedUntil()).isEqualTo(hasta);
        assertThat(cuenta.estaSuspendidaHasta(AHORA)).isTrue();
    }

    @Test
    void otorgarRolModeratorEsIdempotente() {
        Account cuenta = cuenta(AccountStatus.ACTIVE, null);

        cuenta.otorgarRolModerator();
        cuenta.otorgarRolModerator();
        cuenta.otorgarRolModerator();

        assertThat(cuenta.roles()).containsExactlyInAnyOrder(Role.PLAYER, Role.MODERATOR);
    }

    @Test
    void marcarVerificadaSoloFijaElValorLaPrimeraVez() {
        Account cuenta = cuenta(AccountStatus.ACTIVE, null);
        Instant primeraVez = AHORA.plusSeconds(10);
        Instant segundaVez = AHORA.plusSeconds(20);

        assertThat(cuenta.verifiedAt()).isNull();
        assertThat(cuenta.marcarVerificada(primeraVez)).isTrue();
        assertThat(cuenta.verifiedAt()).isEqualTo(primeraVez);
        assertThat(cuenta.marcarVerificada(segundaVez)).isFalse();
        assertThat(cuenta.verifiedAt()).isEqualTo(primeraVez);
    }

    @Test
    void elDisplayNameDebeTenerEntre3Y20Caracteres() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> cuentaNuevaConNombre("ab"))
                .withMessageContaining("displayName");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> cuentaNuevaConNombre("a".repeat(21)))
                .withMessageContaining("displayName");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> cuentaNuevaConNombre("   "))
                .withMessageContaining("displayName");

        assertThat(cuentaNuevaConNombre("abc").displayName()).isEqualTo("abc");
        assertThat(cuentaNuevaConNombre("a".repeat(20)).displayName()).hasSize(20);
    }

    private static Account cuentaNuevaConNombre(String nombre) {
        return new Account(UUID.randomUUID(), "ana@kp.test", nombre,
                Set.of(Role.PLAYER), AccountStatus.ACTIVE, null, "dev:ana@kp.test", null, AHORA);
    }

    @Test
    void elNombreSaneadoConservaSoloAlfanumericosRellenaYTrunca() {
        // TODO(contrato)
        assertThat(Account.nombreSaneado("ana.kp@gmail.com")).isEqualTo("anakp");
        assertThat(Account.nombreSaneado("ab@gmail.com")).isEqualTo("abx");
        assertThat(Account.nombreSaneado("....@kp.test")).isEqualTo("xxx");
        assertThat(Account.nombreSaneado("un_nombre_muy_largo_1234567890@kp.test")).hasSize(20);
        assertThat(Account.nombreSaneado("jugador1@kp.test")).isEqualTo("jugador1");
    }
}
