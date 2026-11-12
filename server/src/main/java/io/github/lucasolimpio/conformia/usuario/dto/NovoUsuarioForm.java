package io.github.lucasolimpio.conformia.usuario.dto;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados do drawer "Novo usuário". Aqui só validação de formato;
 * regras como "e-mail não pode repetir" ficam no service.
 * Sem campo de senha: o sistema gera uma temporária.
 */
public record NovoUsuarioForm(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 3, max = 120, message = "O nome precisa ter entre 3 e 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres.")
        String email,

        @NotNull(message = "Escolha um perfil.")
        Perfil perfil,

        /** Boolean: o Jackson 3 recusaria o JSON inteiro se um boolean primitivo faltasse. */
        Boolean ativo
) {

    /**
     * Ausente no JSON da API = ativo (o normal ao criar alguém).
     * Na web o checkbox sempre manda valor: marcado = true, desmarcado = false (marcador "_ativo").
     */
    public NovoUsuarioForm {
        ativo = ativo == null || ativo;
    }

    /** Valores iniciais do formulário vazio. */
    public static NovoUsuarioForm vazio() {
        return new NovoUsuarioForm("", "", Perfil.ANALISTA, true);
    }
}
