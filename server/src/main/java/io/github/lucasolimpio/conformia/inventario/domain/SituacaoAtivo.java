package io.github.lucasolimpio.conformia.inventario.domain;

public enum SituacaoAtivo {
    EM_USO,
    /** Saiu de uso; continua no banco para histórico e riscos ligados a ele. */
    DESCONTINUADO
}
