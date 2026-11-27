package com.killerprotocol.cuentas.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta de un jugador: agregado raíz del dominio de Cuentas.
 */
public class Account {

    public static final int NOMBRE_MIN = 3;
    public static final int NOMBRE_MAX = 20;

    private final UUID id;
    private final String email;
    private final String googleSubject;
    private final Instant createdAt;
    private final Set<Role> roles;

    private String displayName;
    private AccountStatus status;
    private Instant suspendedUntil;
    private Instant verifiedAt;

    public Account(UUID id, String email, String displayName, Set<Role> roles, AccountStatus status,
                   Instant suspendedUntil, String googleSubject, Instant verifiedAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email no puede estar vacío");
        }
        this.displayName = validarNombre(displayName);
        this.roles = new HashSet<>(Objects.requireNonNull(roles, "roles"));
        if (this.roles.isEmpty()) {
            throw new IllegalArgumentException("la cuenta debe tener al menos un rol");
        }
        this.status = Objects.requireNonNull(status, "status");
        this.suspendedUntil = suspendedUntil;
        this.googleSubject = Objects.requireNonNull(googleSubject, "googleSubject");
        this.verifiedAt = verifiedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public void aplicarSuspencion(Instant hasta) {
        Objects.requireNonNull(hasta, "hasta");
        // TODO(contrato)
        if (esBaneada()) {
            return;
        }
        this.status = AccountStatus.SUSPENDED;
        this.suspendedUntil = hasta;
    }

    public void aplicarBan() {
        this.status = AccountStatus.BANNED;
        this.suspendedUntil = null;
    }

    public boolean levantarSuspencionVencida(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == AccountStatus.SUSPENDED && suspendedUntil != null && !suspendedUntil.isAfter(now)) {
            this.status = AccountStatus.ACTIVE;
            this.suspendedUntil = null;
            return true;
        }
        return false;
    }

    public boolean marcarVerificada(Instant cuando) {
        Objects.requireNonNull(cuando, "cuando");
        if (verifiedAt == null) {
            this.verifiedAt = cuando;
            return true;
        }
        return false;
    }

    public void otorgarRolModerator() {
        roles.add(Role.MODERATOR);
    }

    public void actualizarNombre(String nuevoNombre) {
        this.displayName = validarNombre(nuevoNombre);
    }

    public boolean esBaneada() {
        return status == AccountStatus.BANNED;
    }

    public boolean estaSuspendidaHasta(Instant now) {
        Objects.requireNonNull(now, "now");
        return status == AccountStatus.SUSPENDED && suspendedUntil != null && suspendedUntil.isAfter(now);
    }

    public static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("displayName no puede estar vacío");
        }
        if (nombre.length() < NOMBRE_MIN || nombre.length() > NOMBRE_MAX) {
            throw new IllegalArgumentException(
                    "displayName debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX + " caracteres");
        }
        return nombre;
    }

    /**
    TODO(contrato)
     */
    public static String nombreSaneado(String email) {
        Objects.requireNonNull(email, "email");
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            throw new IllegalArgumentException("email inválido: " + email);
        }
        String saneado = email.substring(0, arroba).replaceAll("[^a-zA-Z0-9]", "");
        while (saneado.length() < NOMBRE_MIN) {
            saneado = saneado + "x";
        }
        return saneado.substring(0, Math.min(NOMBRE_MAX, saneado.length()));
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    public AccountStatus status() {
        return status;
    }

    public Instant suspendedUntil() {
        return suspendedUntil;
    }

    public String googleSubject() {
        return googleSubject;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
