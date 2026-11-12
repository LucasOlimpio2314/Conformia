package io.github.lucasolimpio.conformia.risco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nova ameaça ou vulnerabilidade cadastrada pelo botão "+ Nova" do formulário. */
public record NovoItemCatalogoForm(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 3, max = 150, message = "O nome precisa ter entre 3 e 150 caracteres.")
        String nome
) {
}
