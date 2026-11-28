package com.killerprotocol.cuentas.adapter.in.web.dto;

import com.killerprotocol.cuentas.application.port.in.PerfilPublico;

public record PerfilPublicoResponse(String playerId, String displayName) {

    public static PerfilPublicoResponse de(PerfilPublico perfil) {
        return new PerfilPublicoResponse(perfil.playerId().toString(), perfil.displayName());
    }
}
