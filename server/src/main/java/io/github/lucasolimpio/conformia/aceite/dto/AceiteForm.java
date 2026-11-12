package io.github.lucasolimpio.conformia.aceite.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Drawer "Aceite formal". A declaração precisa estar marcada. */
public record AceiteForm(
        @NotBlank(message = "Explique o aceite.")
        @Size(min = 20, max = 1000, message = "A justificativa precisa ter entre 20 e 1000 caracteres.")
        String justificativa,

        @Size(max = 1000, message = "As condições podem ter no máximo 1000 caracteres.")
        String condicoes,

        @AssertTrue(message = "Marque a declaração de aceite.")
        Boolean declaracao
) {

    public AceiteForm {
        declaracao = Boolean.TRUE.equals(declaracao);
    }

    public static AceiteForm vazio() {
        return new AceiteForm("", "", false);
    }
}
