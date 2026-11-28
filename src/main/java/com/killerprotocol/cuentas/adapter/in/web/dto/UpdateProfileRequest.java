package com.killerprotocol.cuentas.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank(message = "displayName no puede estar vacío")
        @Pattern(regexp = ".{3,20}", message = "displayName debe tener entre 3 y 20 caracteres")
        String displayName) {
}
