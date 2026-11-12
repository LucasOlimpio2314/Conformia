package io.github.lucasolimpio.conformia.risco.dto;

/**
 * Ativo que pode ser escolhido no formulário do risco. criticidade sugere o impacto;
 * proprietarioId sugere o dono do risco.
 */
public record AtivoOpcao(Long id, String codigo, String nome, int criticidade, Long proprietarioId, boolean emUso) {
}
