package com.killerprotocol.cuentas.adapter.out.jwt;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.Sha256;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class NimbusTokenIssuer implements TokenIssuer {

    private final KeyStore clave;
    private final String issuer;
    private final String audience;
    private final long ttlSec;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public NimbusTokenIssuer(KeyStore clave, String issuer, String audience, long ttlSec, Clock clock) {
        this.clave = Objects.requireNonNull(clave, "clave");
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer no puede estar vacío");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("audience no puede estar vacío");
        }
        if (ttlSec <= 0) {
            throw new IllegalArgumentException("ttlSec debe ser positivo");
        }
        this.issuer = issuer;
        this.audience = audience;
        this.ttlSec = ttlSec;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String emitirAccess(Account cuenta) {
        Objects.requireNonNull(cuenta, "cuenta");
        Instant ahora = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(cuenta.id().toString())
                .claim("roles", nombresDeRoles(cuenta))
                .claim("name", cuenta.displayName())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plusSeconds(ttlSec)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.kid()).build(),
                claims);
        try {
            jwt.sign(new RSASSASigner(clave.privateKey()));
        } catch (JOSEException e) {
            throw new IllegalStateException("no se pudo firmar el JWT", e);
        }
        return jwt.serialize();
    }

    private static List<String> nombresDeRoles(Account cuenta) {
        List<String> roles = new ArrayList<>();
        for (Role rol : Role.values()) {
            if (cuenta.roles().contains(rol)) {
                roles.add(rol.name());
            }
        }
        return List.copyOf(roles);
    }

    @Override
    public RefreshEmitido nuevoRefresh(Account cuenta, UUID familyId, Duration ttl) {
        Objects.requireNonNull(cuenta, "cuenta");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl debe ser positivo");
        }
        String valorOpaco = valorAleatorio();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                cuenta.id(),
                Sha256.sha256Hex(valorOpaco),
                familyId != null ? familyId : UUID.randomUUID(),
                clock.instant().plus(ttl),
                clock);
        return new RefreshEmitido(valorOpaco, token);
    }

    private String valorAleatorio() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
