package com.killerprotocol.cuentas.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.killerprotocol.cuentas.application.port.in.Cuenta;
import com.killerprotocol.cuentas.application.port.in.GetAccountUseCase;
import com.killerprotocol.cuentas.application.port.in.ListPublicProfilesUseCase;
import com.killerprotocol.cuentas.application.port.in.PerfilPublico;
import com.killerprotocol.cuentas.application.port.in.UpdateProfileUseCase;
import com.killerprotocol.cuentas.domain.exception.CuentaNoEncontradaException;
import com.killerprotocol.cuentas.domain.model.AccountStatus;
import com.killerprotocol.cuentas.domain.model.Role;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(HeaderJugadorResolver.class)
class AccountControllerWebMvcTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String HEADER = HeaderJugadorResolver.HEADER;
    private static final Instant CREADO = Instant.parse("2026-11-25T19:10:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetAccountUseCase cuentaPropia;

    @MockBean
    private UpdateProfileUseCase perfil;

    @MockBean
    private ListPublicProfilesUseCase publicos;

    private final UUID playerId = UUID.randomUUID();

    private Cuenta cuenta() {
        return new Cuenta(playerId, "ana.kp@gmail.com", "anakp",
                Set.of(Role.PLAYER, Role.MODERATOR), AccountStatus.ACTIVE, CREADO);
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/me")
    class Me {

        @Test
        @DisplayName("200: cuenta completa (playerId, email, displayName, roles, status, createdAt)")
        void ok() throws Exception {
            when(cuentaPropia.me(playerId)).thenReturn(cuenta());

            mvc.perform(get("/api/v1/accounts/me").header(HEADER, playerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                    .andExpect(jsonPath("$.email").value("ana.kp@gmail.com"))
                    .andExpect(jsonPath("$.displayName").value("anakp"))
                    .andExpect(jsonPath("$.roles[0]").value("PLAYER"))
                    .andExpect(jsonPath("$.roles[1]").value("MODERATOR"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.createdAt").value(CREADO.toString()));
        }

        @Test
        @DisplayName("401 problem+json cuando falta el header X-Player-Id")
        void sinHeaderDa401() throws Exception {
            mvc.perform(get("/api/v1/accounts/me")
                            .header("X-Request-Id", "req-401"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/jugador-no-identificado"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.instance").value("/api/v1/accounts/me"))
                    .andExpect(jsonPath("$.traceId").value("req-401"));
        }

        @Test
        @DisplayName("404 cuando la cuenta no existe")
        void inexistenteDa404() throws Exception {
            when(cuentaPropia.me(playerId))
                    .thenThrow(new CuentaNoEncontradaException(playerId.toString()));

            mvc.perform(get("/api/v1/accounts/me").header(HEADER, playerId.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/cuenta-no-encontrada"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/accounts/me")
    class ActualizarMe {

        @Test
        @DisplayName("200: devuelve la cuenta con el nuevo displayName")
        void ok() throws Exception {
            Cuenta actualizada = new Cuenta(playerId, "ana.kp@gmail.com", "Kouzou",
                    Set.of(Role.PLAYER), AccountStatus.ACTIVE, CREADO);
            when(perfil.update(playerId, "Kouzou")).thenReturn(actualizada);

            mvc.perform(patch("/api/v1/accounts/me")
                            .header(HEADER, playerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"Kouzou\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Kouzou"));
        }

        @Test
        @DisplayName("400 con errors[] cuando el displayName tiene 21 caracteres")
        void nombreDe21CaracteresDa400ConErrors() throws Exception {
            mvc.perform(patch("/api/v1/accounts/me")
                            .header(HEADER, playerId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayName\":\"" + "a".repeat(21) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/validacion"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors[0].field").value("displayName"))
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/public?ids=")
    class Publicos {

        @Test
        @DisplayName("200: array de playerId + displayName")
        void ok() throws Exception {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            when(publicos.listar(anySet()))
                    .thenReturn(List.of(new PerfilPublico(a, "anakp"),
                            new PerfilPublico(b, "bobkp")));

            mvc.perform(get("/api/v1/accounts/public")
                            .param("ids", a + "," + b))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].playerId").value(a.toString()))
                    .andExpect(jsonPath("$[0].displayName").value("anakp"))
                    .andExpect(jsonPath("$[1].playerId").value(b.toString()))
                    .andExpect(jsonPath("$[1].displayName").value("bobkp"))
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("400 con 21 ids (límite de 20)")
        void veintiunIdsDan400() throws Exception {

            when(publicos.listar(anySet()))
                    .thenThrow(new IllegalArgumentException("máximo 20 ids por consulta"));

            String ids = IntStream.range(0, 21)
                    .mapToObj(i -> UUID.randomUUID().toString())
                    .collect(Collectors.joining(","));

            mvc.perform(get("/api/v1/accounts/public").param("ids", ids))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", PROBLEM_JSON))
                    .andExpect(jsonPath("$.type")
                            .value("https://killerprotocol.dev/problems/validacion"))
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
