package io.github.lucasolimpio.conformia.inventario.dto;

/** Body de POST /api/ativos/{id}/descontinuar. A regra do tamanho fica no service. */
public record DescontinuarForm(String justificativa) {
}
