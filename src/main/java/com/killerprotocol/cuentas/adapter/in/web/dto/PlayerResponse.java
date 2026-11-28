package com.killerprotocol.cuentas.adapter.in.web.dto;

import java.util.List;

public record PlayerResponse(String playerId, String displayName, List<String> roles) {
}
