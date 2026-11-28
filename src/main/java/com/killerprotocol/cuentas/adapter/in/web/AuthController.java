package com.killerprotocol.cuentas.adapter.in.web;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.killerprotocol.cuentas.adapter.in.web.dto.ChallengeResponse;
import com.killerprotocol.cuentas.adapter.in.web.dto.GoogleLoginRequest;
import com.killerprotocol.cuentas.adapter.in.web.dto.LogoutRequest;
import com.killerprotocol.cuentas.adapter.in.web.dto.PlayerResponse;
import com.killerprotocol.cuentas.adapter.in.web.dto.RefreshRequest;
import com.killerprotocol.cuentas.adapter.in.web.dto.SessionResponse;
import com.killerprotocol.cuentas.adapter.in.web.dto.TwoFactorVerifyRequest;
import com.killerprotocol.cuentas.application.port.in.AuthenticateWithGoogleUseCase;
import com.killerprotocol.cuentas.application.port.in.ChallengeCreado;
import com.killerprotocol.cuentas.application.port.in.LogoutUseCase;
import com.killerprotocol.cuentas.application.port.in.RefreshSessionUseCase;
import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.port.in.VerifyTwoFactorUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateWithGoogleUseCase conGoogle;
    private final VerifyTwoFactorUseCase dosFactores;
    private final RefreshSessionUseCase refresco;
    private final LogoutUseCase cierre;

    public AuthController(AuthenticateWithGoogleUseCase conGoogle,
                          VerifyTwoFactorUseCase dosFactores,
                          RefreshSessionUseCase refresco,
                          LogoutUseCase cierre) {
        this.conGoogle = conGoogle;
        this.dosFactores = dosFactores;
        this.refresco = refresco;
        this.cierre = cierre;
    }

    @PostMapping("/google")
    public ChallengeResponse google(@Valid @RequestBody GoogleLoginRequest req) {
        ChallengeCreado creado = conGoogle.authenticate(req.idToken());
        return new ChallengeResponse(creado.challengeId().toString(), creado.channel(),
                creado.expiresInSec());
    }

    @PostMapping("/2fa/verify")
    public SessionResponse verificar(@Valid @RequestBody TwoFactorVerifyRequest req) {
        Sesion sesion = dosFactores.verify(UUID.fromString(req.challengeId()), req.code());
        return responder(sesion);
    }

    @PostMapping("/refresh")
    public SessionResponse refrescar(@Valid @RequestBody RefreshRequest req) {
        Sesion sesion = refresco.refresh(req.refreshToken());
        return responder(sesion);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrar(@Valid @RequestBody LogoutRequest req) {
        cierre.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private static SessionResponse responder(Sesion s) {
        List<String> roles = s.roles().stream()
                .sorted(Comparator.comparingInt(role -> role.ordinal()))
                .map(role -> role.name())
                .toList();
        PlayerResponse player = new PlayerResponse(s.playerId().toString(), s.displayName(), roles);
        return new SessionResponse(s.accessToken(), s.refreshToken(), s.expiresInSec(), player);
    }
}
