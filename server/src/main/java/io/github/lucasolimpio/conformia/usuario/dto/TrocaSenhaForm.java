package io.github.lucasolimpio.conformia.usuario.dto;

/**
 * Troca de senha. {@code senhaAtual} só é usada em "Minha conta"; na troca
 * obrigatória após senha temporária a pessoa acabou de digitá-la no login.
 * As regras da nova senha ficam no service (uma regra, dois clientes).
 */
public record TrocaSenhaForm(String senhaAtual, String novaSenha, String confirmacao) {

    public static TrocaSenhaForm vazio() {
        return new TrocaSenhaForm("", "", "");
    }
}
