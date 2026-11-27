package com.killerprotocol.cuentas.adapter.out.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.killerprotocol.cuentas.application.port.out.TokenIssuer;
import com.killerprotocol.cuentas.domain.model.Account;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.RefreshToken;
import com.killerprotocol.cuentas.domain.model.Role;
import com.killerprotocol.cuentas.domain.model.Sha256;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;

class TokenIssuerTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private KeyStore clave;
    private NimbusTokenIssuer emisor;

    @BeforeEach
    void preparar() {
        clave = new KeyStore(tempDir.resolve("private.pem"));
        emisor = new NimbusTokenIssuer(clave, "killer-cuentas", "killer-api", 900, RELOJ);
    }

    private Account cuentaConRoles(Set<Role> roles) {
        return new Account(UUID.randomUUID(), "ana@kp.test", "anakp", roles, AccountStatus.ACTIVE,
                null, "dev:ana@kp.test", null, AHORA);
    }

    @Test
    void emiteJwtsRs256ConFirmaValidaYClaimsExactos() throws Exception {
        Account cuenta = cuentaConRoles(Set.of(Role.PLAYER));
        String jwt = emisor.emitirAccess(cuenta);

        SignedJWT parsed = SignedJWT.parse(jwt);

        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(parsed.getHeader().getKeyID()).isEqualTo(clave.kid());
        assertThat(parsed.verify(new RSASSAVerifier(clave.publicKey()))).isTrue();

        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("killer-cuentas");
        assertThat(parsed.getJWTClaimsSet().getAudience()).containsExactly("killer-api");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo(cuenta.id().toString());
        assertThat(parsed.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("PLAYER");
        assertThat(parsed.getJWTClaimsSet().getStringClaim("name")).isEqualTo("anakp");
        assertThat(parsed.getJWTClaimsSet().getJWTID()).isNotBlank();

        long iat = parsed.getJWTClaimsSet().getIssueTime().getTime() / 1000;
        long exp = parsed.getJWTClaimsSet().getExpirationTime().getTime() / 1000;
        assertThat(exp - iat).isPositive().isEqualTo(900).isLessThanOrEqualTo(15 * 60);
    }

    @Test
    void exponeLosRolesConOrdenEstableIncluyendoModerator() throws Exception {
        Account cuenta = cuentaConRoles(Set.of(Role.MODERATOR, Role.PLAYER));

        SignedJWT parsed = SignedJWT.parse(emisor.emitirAccess(cuenta));

        assertThat(parsed.getJWTClaimsSet().getStringListClaim("roles"))
                .containsExactly("PLAYER", "MODERATOR");
        assertThat(parsed.verify(new RSASSAVerifier(clave.publicKey()))).isTrue();
    }

    @Test
    void elJwtSeValidaConLaClavePublicaDelJwks() throws Exception {
        String jwt = emisor.emitirAccess(cuentaConRoles(Set.of(Role.PLAYER)));

        JWKSet jwks = JWKSet.parse(clave.jwksJson());
        RSAKey jwk = (RSAKey) jwks.getKeys().getFirst();
        assertThat(jwk.getKeyID()).isEqualTo(clave.kid());

        assertThat(SignedJWT.parse(jwt).verify(new RSASSAVerifier(jwk.toRSAPublicKey()))).isTrue();
    }

    @Test
    void elRefreshTokenEsOpacoEIndependienteDelJwt() {
        Account cuenta = cuentaConRoles(Set.of(Role.PLAYER));
        UUID familia = UUID.randomUUID();

        TokenIssuer.RefreshEmitido refresh = emisor.nuevoRefresh(cuenta, familia, Duration.ofDays(7));

        assertThat(refresh.valorOpaco()).doesNotContain(".");

        RefreshToken token = refresh.token();
        assertThat(token.tokenHash()).isEqualTo(Sha256.sha256Hex(refresh.valorOpaco()));
        assertThat(token.familyId()).isEqualTo(familia);
        assertThat(token.accountId()).isEqualTo(cuenta.id());
        assertThat(token.expiresAt()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
        assertThat(token.esRotado()).isFalse();
        assertThat(token.esRevocado()).isFalse();
    }

    @Test
    void sinFamiliaSeCreaUnaNueva() {
        TokenIssuer.RefreshEmitido refresh =
                emisor.nuevoRefresh(cuentaConRoles(Set.of(Role.PLAYER)), null, Duration.ofDays(7));
        assertThat(refresh.token().familyId()).isNotNull();
    }
}
