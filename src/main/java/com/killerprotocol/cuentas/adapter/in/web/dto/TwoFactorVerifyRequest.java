package com.killerprotocol.cuentas.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos") String code) {
}
