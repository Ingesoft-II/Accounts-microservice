package com.killerprotocol.cuentas.adapter.out.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

public class KeyStore {

    private static final String PRIVATE_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_END = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_END = "-----END PUBLIC KEY-----";

    private final Path keyFile;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String kid;

    public KeyStore(Path keyFile) {
        this.keyFile = Objects.requireNonNull(keyFile, "keyFile");
        asegurarArchivo(keyFile);
        try {
            String contenido = Files.readString(keyFile, StandardCharsets.UTF_8);
            this.privateKey = cargarPrivada(contenido);
            this.publicKey = cargarPublica(contenido);
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo leer la clave de " + keyFile, e);
        }
        this.kid = kidDe(publicKey);
    }

    private void asegurarArchivo(Path file) {
        if (Files.exists(file)) {
            return;
        }
        try {
            Path directorio = file.toAbsolutePath().getParent();
            if (directorio != null) {
                Files.createDirectories(directorio);
            }
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            String pem = PRIVATE_BEGIN + "\n" + base64Pem(par.getPrivate().getEncoded())
                    + PRIVATE_END + "\n"
                    + PUBLIC_BEGIN + "\n" + base64Pem(par.getPublic().getEncoded())
                    + PUBLIC_END + "\n";
            Files.writeString(file, pem, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA no está disponible en esta JVM", e);
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo crear el archivo de la clave en " + file, e);
        }
    }

    private static String base64Pem(byte[] der) {
        return Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
    }

    private static String extraerPem(String contenido, String inicio, String fin) {
        int desde = contenido.indexOf(inicio);
        if (desde < 0) {
            return null;
        }
        int hasta = contenido.indexOf(fin, desde);
        if (hasta < 0) {
            return null;
        }
        return contenido.substring(desde + inicio.length(), hasta).replaceAll("\\s", "");
    }

    private static KeyFactory keyFactoryRsa() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA no está disponible en esta JVM", e);
        }
    }

    private static RSAPrivateKey cargarPrivada(String contenido) {
        String base64 = extraerPem(contenido, PRIVATE_BEGIN, PRIVATE_END);
        if (base64 == null) {
            throw new IllegalStateException("el archivo de la clave no contiene una clave privada PKCS#8");
        }
        try {
            return (RSAPrivateKey) keyFactoryRsa().generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("la clave privada no es una RSA válida en PKCS#8", e);
        }
    }

    private static RSAPublicKey cargarPublica(String contenido) {
        String base64 = extraerPem(contenido, PUBLIC_BEGIN, PUBLIC_END);
        if (base64 == null) {
            throw new IllegalStateException(
                    "el archivo de la clave no contiene el bloque PUBLIC KEY; regenéralo");
        }
        try {
            return (RSAPublicKey) keyFactoryRsa().generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("la clave pública no es una RSA válida en X.509", e);
        }
    }

    /**
     * {@code kid} = base64url (sin padding) de SHA-256 aplicada a la DER (X.509)
     * de la clave pública.
     */
    static String kidDe(RSAPublicKey clavePublica) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] huella = sha256.digest(clavePublica.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(huella);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
        }
    }

    public String jwksJson() {
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new JWKSet(jwk).toString();
    }

    public String kid() {
        return kid;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public Path keyFile() {
        return keyFile;
    }
}
