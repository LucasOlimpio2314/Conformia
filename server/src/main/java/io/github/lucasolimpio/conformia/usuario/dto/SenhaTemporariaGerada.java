package io.github.lucasolimpio.conformia.usuario.dto;

/**
 * Resultado de criar usuário ou redefinir senha. É o único momento em que a
 * senha em texto aberto existe: vai para a tela uma vez e não é guardada.
 */
public record SenhaTemporariaGerada(Long usuarioId, String nomeUsuario, String senha) {
}
