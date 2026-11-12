package io.github.lucasolimpio.conformia.aceite.dto;

/** Body de POST /api/aceites/{id}/revogar. Tamanho mínimo é regra do service. */
public record RevogacaoForm(String motivo) {
}
