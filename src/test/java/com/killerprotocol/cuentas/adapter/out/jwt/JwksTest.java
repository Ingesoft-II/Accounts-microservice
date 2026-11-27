package com.killerprotocol.cuentas.adapter.out.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.KeyUse;

class JwksTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    private KeyStore clave;

    @BeforeEach
    void preparar() {
        clave = new KeyStore(tempDir.resolve("private.pem"));
    }

    @Test
    void elJwksSoloExponeLaClavePublica() throws Exception {
        JsonNode jwks = MAPPER.readTree(clave.jwksJson());

        JsonNode keys = jwks.get("keys");
        assertThat(keys.isArray()).isTrue();
        assertThat(keys.size()).isEqualTo(1);
        JsonNode jwk = keys.get(0);

        assertThat(jwk.get("kty").asText()).isEqualTo("RSA");
        assertThat(jwk.get("alg").asText()).isEqualTo("RS256");
        assertThat(jwk.get("use").asText()).isEqualTo(KeyUse.SIGNATURE.getValue());
        assertThat(jwk.get("kid").asText()).isEqualTo(clave.kid());

        assertThat(jwk.get("n").asText()).isEqualTo(b64url(clave.publicKey().getModulus()));
        assertThat(jwk.get("e").asText()).isEqualTo(b64url(clave.publicKey().getPublicExponent()));

        byte[] n = Base64.getUrlDecoder().decode(jwk.get("n").asText());
        assertThat(new BigInteger(1, n).bitLength()).isEqualTo(2048);
        assertThat(jwk.get("e").asText()).isEqualTo(Base64.getUrlEncoder().withoutPadding()
                .encodeToString(BigInteger.valueOf(65537).toByteArray()));

        List<String> privados = List.of("d", "p", "q", "dp", "dq", "qi", "oth");
        for (String campo : privados) {
            assertThat(jwk.has(campo)).as("el JWKS no debe contener el campo privado <%s>", campo).isFalse();
        }
        assertThat(MAPPER.writeValueAsString(jwks))
                .doesNotContain("\"d\"", "\"p\"", "\"q\"", "\"dp\"", "\"dq\"", "\"qi\"");
    }

    @Test
    void generaLaClaveUnaSolaVezYPersisteLaMisma() throws Exception {

        KeyStore segunda = new KeyStore(tempDir.resolve("private.pem"));
        assertThat(segunda.kid()).isEqualTo(clave.kid());
        assertThat(segunda.publicKey().getModulus()).isEqualTo(clave.publicKey().getModulus());

        assertThat(MAPPER.readTree(segunda.jwksJson()).get("keys").get(0).get("kid").asText())
                .isEqualTo(clave.kid());
    }

    @Test
    void elKidEsBase64urlDeSha256DeLaDerDeLaPublica() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();

        String esperado = KeyStore.kidDe((java.security.interfaces.RSAPublicKey) par.getPublic());

        byte[] sha256 = java.security.MessageDigest.getInstance("SHA-256")
                .digest(par.getPublic().getEncoded());
        String manual = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256);

        assertThat(esperado).isEqualTo(manual)
                .doesNotContain("=").doesNotContain("+").doesNotContain("/").isNotBlank();
    }

    private static String b64url(BigInteger valor) {
        byte[] bytes = valor.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
