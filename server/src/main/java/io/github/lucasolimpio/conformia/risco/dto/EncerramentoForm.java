package io.github.lucasolimpio.conformia.risco.dto;

/** Body de POST /api/riscos/{id}/encerrar. O tamanho mínimo é regra do service. */
public record EncerramentoForm(String justificativa) {
}
