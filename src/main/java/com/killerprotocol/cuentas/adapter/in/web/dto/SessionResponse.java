package com.killerprotocol.cuentas.adapter.in.web.dto;

public record SessionResponse(String accessToken, String refreshToken, int expiresInSec,
                              PlayerResponse player) {
}
