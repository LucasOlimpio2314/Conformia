package io.github.lucasolimpio.conformia.usuario.dto;

/**
 * Resposta do login e do /api/auth/me. Se {@code trocaSenhaObrigatoria}
 * for true, o app deve abrir a tela de troca de senha: as outras rotas
 * respondem 403 TROCA_SENHA_OBRIGATORIA até a troca.
 */
public record SessaoResposta(UsuarioResposta usuario, boolean trocaSenhaObrigatoria) {
}
