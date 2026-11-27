package com.killerprotocol.cuentas.config;

import java.nio.file.Path;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.killerprotocol.cuentas.adapter.out.jwt.KeyStore;
import com.killerprotocol.cuentas.adapter.out.jwt.NimbusTokenIssuer;
import com.killerprotocol.cuentas.application.port.out.TokenIssuer;

@Configuration
public class CuentasConfig {

    @Bean
    public KeyStore keyStore(CuentasProperties properties) {
        return new KeyStore(Path.of(properties.jwt().keyPath()));
    }

    @Bean
    public TokenIssuer tokenIssuer(KeyStore keyStore, CuentasProperties properties) {
        CuentasProperties.Jwt jwt = properties.jwt();
        return new NimbusTokenIssuer(keyStore, jwt.issuer(), jwt.audience(), jwt.ttlSec(), Clock.systemUTC());
    }
}
