package io.github.lucasolimpio.conformia.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edição de usuário pelo Gerente: nome, e-mail e se o acesso está ativo.
 * Perfil e senha não mudam aqui (senha: "Redefinir senha" ou "Minha conta").
 * {@code ativo} é obrigatório no JSON para a API não desativar ninguém por omissão.
 */
public record EdicaoUsuarioForm(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 3, max = 120, message = "O nome precisa ter entre 3 e 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres.")
        String email,

        @NotNull(message = "Informe se o acesso está ativo.")
        Boolean ativo
) {

    public static EdicaoUsuarioForm de(UsuarioResumo u) {
        return new EdicaoUsuarioForm(u.nome(), u.email(), u.ativo());
    }
}
