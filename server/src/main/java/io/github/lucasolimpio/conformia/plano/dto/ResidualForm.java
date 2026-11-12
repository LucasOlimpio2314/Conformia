package io.github.lucasolimpio.conformia.plano.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Risco residual esperado depois do plano (matriz 5x5). */
public record ResidualForm(
        @NotNull(message = "Escolha a probabilidade.") @Min(1) @Max(5) Integer probabilidade,
        @NotNull(message = "Escolha o impacto.") @Min(1) @Max(5) Integer impacto
) {
}
