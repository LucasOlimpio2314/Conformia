package io.github.lucasolimpio.conformia.aviso.domain;

public enum TipoAviso {
    /** O risco mudou depois do aceite: o aceite caiu. */
    ACEITE_INVALIDADO,
    /** O aceite passou dos 12 meses. */
    ACEITE_VENCIDO,
    /** Todas as ações do plano terminaram: o residual aguarda aceite. */
    RESIDUAL_PRONTO
}
