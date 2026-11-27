package com.killerprotocol.cuentas.adapter.in.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.killerprotocol.cuentas.adapter.out.jwt.KeyStore;

@RestController
public class JwksController {

    private final KeyStore clave;

    public JwksController(KeyStore clave) {
        this.clave = clave;
    }

    @GetMapping(path = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String jwks() {
        return clave.jwksJson();
    }
}
