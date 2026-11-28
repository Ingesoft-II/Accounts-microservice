package com.killerprotocol.cuentas.adapter.in.web;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.killerprotocol.cuentas.application.port.in.AuthenticateWithGoogleUseCase;
import com.killerprotocol.cuentas.application.port.in.ChallengeCreado;
import com.killerprotocol.cuentas.application.port.in.LogoutUseCase;
import com.killerprotocol.cuentas.application.port.in.RefreshSessionUseCase;
import com.killerprotocol.cuentas.application.port.in.Sesion;
import com.killerprotocol.cuentas.application.port.in.VerifyTwoFactorUseCase;
import com.killerprotocol.cuentas.domain.exception.CuentaBaneadaException;
import com.killerprotocol.cuentas.domain.exception.CuentaSuspendidaException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException;
import com.killerprotocol.cuentas.domain.exception.DesafioInutilizableException.Motivo;
import com.killerprotocol.cuentas.domain.exception.LimiteDesafiosExcedidoException;
import com.killerprotocol.cuentas.domain.exception.RefrescoInvalidoException;
import com.killerprotocol.cuentas.domain.model.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerWebMvcTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthenticateWithGoogleUseCase conGoogle;

    @MockBean
    private VerifyTwoFactorUseCase dosFactores;

    @MockBean
    private RefreshSessionUseCase refresco;

    @MockBean
    private LogoutUseCase cierre;

    private final UUID challengeId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    private Sesion sesion() {
        return new Sesion("access-jwt", "refresh-opaco", 900, playerId, "ana",
                Set.of(Role.PLAYER, Role.MODERATOR));
    }

    @Nested
    @DisplayName("POST /api/v1/auth/google")
    class Google {

        @Test
        @DisplayName("200: crea el desafío y devuelve challengeId, channel y expiresInSec")
        void ok() throws Exception {
            when(conGoogle.authenticate("id-token"))
                    .thenReturn(new ChallengeCreado(challengeId, "EMAIL", 300));

            mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"id-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.challengeId").value(challengeId.toString()))
                    .andExpect(jsonPath("$.channel").value("EMAIL"))
                    .andExpect(jsonPath("$.expiresInSec").value(300));
        }

        @Test
        @DisplayName("400 con errors[] cuando idToken está vacío")
        void idTokenEnBlancoDa400ConErrors() throws Exception {
            mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Request-Id", "req-400")
                            .content("{\"idToken\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/validacion"))
                    .andExpect(jsonPath("$.title").value("Datos de entrada inválidos"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.instance").value("/api/v1/auth/google"))
                    .andExpect(jsonPath("$.traceId").value("req-400"))
                    .andExpect(jsonPath("$.errors[0].field").value("idToken"))
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/2fa/verify")
    class Verificar2Fa {

        @Test
        @DisplayName("200: emite la sesión con accessToken, refreshToken y player")
        void ok() throws Exception {
            when(dosFactores.verify(challengeId, "123456")).thenReturn(sesion());

            mvc.perform(post("/api/v1/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-opaco"))
                    .andExpect(jsonPath("$.expiresInSec").value(900))
                    .andExpect(jsonPath("$.player.playerId").value(playerId.toString()))
                    .andExpect(jsonPath("$.player.displayName").value("ana"))
                    .andExpect(jsonPath("$.player.roles").isArray())
                    .andExpect(jsonPath("$.player.roles[0]").value("PLAYER"))
                    .andExpect(jsonPath("$.player.roles[1]").value("MODERATOR"));
        }

        @Test
        @DisplayName("400 con errors[] cuando el code no son 6 dígitos")
        void codeInvalidoDa400ConErrors() throws Exception {
            mvc.perform(post("/api/v1/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"12a\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/validacion"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors[0].field").value("code"));
        }

        @Test
        @DisplayName("401 cuando el desafío expiró (problem+json con traceId)")
        void desafioExpiradoDa401() throws Exception {
            when(dosFactores.verify(any(), any()))
                    .thenThrow(new DesafioInutilizableException(Motivo.EXPIRADO,
                            "el desafío expiró"));

            mvc.perform(post("/api/v1/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Request-Id", "req-401")
                            .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"123456\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/desafio-inutilizable"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.instance").value("/api/v1/auth/2fa/verify"))
                    .andExpect(jsonPath("$.traceId").value("req-401"));
        }

        @Test
        @DisplayName("429 cuando el desafío se agotó por intentos")
        void desafioAgotadoDa429() throws Exception {
            when(dosFactores.verify(any(), any()))
                    .thenThrow(new DesafioInutilizableException(Motivo.AGOTADO,
                            "se superaron los intentos permitidos"));

            mvc.perform(post("/api/v1/auth/2fa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"challengeId\":\"" + challengeId + "\",\"code\":\"123456\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/desafio-agotado"))
                    .andExpect(jsonPath("$.status").value(429));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refrescar {

        @Test
        @DisplayName("200: devuelve una nueva sesión")
        void ok() throws Exception {
            when(refresco.refresh("refresh-antiguo")).thenReturn(sesion());

            mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh-antiguo\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-opaco"))
                    .andExpect(jsonPath("$.player.playerId").value(playerId.toString()));
        }

        @Test
        @DisplayName("401 cuando el refresh token es inválido")
        void refreshInvalidoDa401() throws Exception {
            when(refresco.refresh(any()))
                    .thenThrow(new RefrescoInvalidoException());

            mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Request-Id", "req-401b")
                            .content("{\"refreshToken\":\"refresh-antiguo\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/refresco-invalido"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.traceId").value("req-401b"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Cerrar {

        @Test
        @DisplayName("204: revoca el refresh token sin cuerpo")
        void ok() throws Exception {
            mvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"refresh-opaco\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("400 con errors[] cuando refreshToken está vacío")
        void refreshTokenEnBlancoDa400() throws Exception {
            mvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
        }
    }

    @Nested
    @DisplayName("Errores de estado de cuenta (403 y 429)")
    class EstadosDeCuenta {

        @Test
        @DisplayName("403 cuenta-baneada cuando la cuenta está BANNED")
        void cuentaBaneadaDa403() throws Exception {
            when(conGoogle.authenticate(any())).thenThrow(new CuentaBaneadaException());

            mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Request-Id", "req-403")
                            .content("{\"idToken\":\"id-token\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/cuenta-baneada"))
                    .andExpect(jsonPath("$.title").value("Cuenta baneada"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.instance").value("/api/v1/auth/google"))
                    .andExpect(jsonPath("$.traceId").value("req-403"));
        }

        @Test
        @DisplayName("403 cuenta-suspendida con until cuando la cuenta está SUSPENDED")
        void cuentaSuspendidaDa403ConUntil() throws Exception {
            Instant until = Instant.parse("2026-11-28T12:00:00Z");
            when(conGoogle.authenticate(any()))
                    .thenThrow(new CuentaSuspendidaException(until));

            mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"id-token\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/cuenta-suspendida"))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.until").value("2026-11-28T12:00:00Z"));
        }

        @Test
        @DisplayName("429 limite-desafios cuando se superan 3 desafíos en la ventana")
        void limiteDesafiosDa429() throws Exception {
            when(conGoogle.authenticate(any()))
                    .thenThrow(new LimiteDesafiosExcedidoException());

            mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"id-token\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/limite-desafios"))
                    .andExpect(jsonPath("$.status").value(429));
        }

        @Test
        @DisplayName("el problema de 400 no incluye traceId si no se envía X-Request-Id")
        void sinHeaderNoHayTraceId() throws Exception {
            String body = mvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idToken\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("traceId");
        }
    }
}
