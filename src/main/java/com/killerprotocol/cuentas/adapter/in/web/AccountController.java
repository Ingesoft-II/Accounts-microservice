package com.killerprotocol.cuentas.adapter.in.web;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.killerprotocol.cuentas.adapter.in.web.dto.CuentaResponse;
import com.killerprotocol.cuentas.adapter.in.web.dto.PerfilPublicoResponse;
import com.killerprotocol.cuentas.adapter.in.web.dto.UpdateProfileRequest;
import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.port.in.GetAccountUseCase;
import com.killerprotocol.cuentas.application.port.in.ListPublicProfilesUseCase;
import com.killerprotocol.cuentas.application.port.in.PerfilPublico;
import com.killerprotocol.cuentas.application.port.in.UpdateProfileUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetAccountUseCase cuentaPropia;
    private final UpdateProfileUseCase perfil;
    private final ListPublicProfilesUseCase publicos;
    private final HeaderJugadorResolver identidad;

    public AccountController(GetAccountUseCase cuentaPropia,
                             UpdateProfileUseCase perfil,
                             ListPublicProfilesUseCase publicos,
                             HeaderJugadorResolver identidad) {
        this.cuentaPropia = cuentaPropia;
        this.perfil = perfil;
        this.publicos = publicos;
        this.identidad = identidad;
    }

    @GetMapping("/me")
    public ResponseEntity<CuentaResponse> me(@RequestHeader HttpHeaders headers) {
        Cuenta propia = cuentaPropia.me(playerId(headers));
        return ResponseEntity.ok(CuentaResponse.de(propia));
    }

    @PatchMapping("/me")
    public ResponseEntity<CuentaResponse> actualizar(@RequestHeader HttpHeaders headers,
                                                      @Valid @RequestBody UpdateProfileRequest req) {
        Cuenta actualizada = perfil.update(playerId(headers), req.displayName());
        return ResponseEntity.ok(CuentaResponse.de(actualizada));
    }

    @GetMapping("/public")
    public ResponseEntity<List<PerfilPublicoResponse>> publicos(@RequestParam("ids") String ids) {
        List<PerfilPublico> perfiles = publicos.listar(parsearIds(ids));
        List<PerfilPublicoResponse> respuesta = perfiles.stream()
                .map(PerfilPublicoResponse::de)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    private UUID playerId(HttpHeaders headers) {
        return identidad.resolver(headers).playerId();
    }

    private static Set<UUID> parsearIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new IllegalArgumentException("ids no puede estar vacío");
        }
        Set<UUID> resultado = new LinkedHashSet<>();
        for (String token : Arrays.asList(ids.split(","))) {
            String limpio = token.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            try {
                resultado.add(UUID.fromString(limpio));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("id inválido: " + limpio);
            }
        }
        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("ids no puede estar vacío");
        }
        return resultado;
    }
}
