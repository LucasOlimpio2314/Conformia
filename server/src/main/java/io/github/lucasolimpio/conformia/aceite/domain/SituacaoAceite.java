package io.github.lucasolimpio.conformia.aceite.domain;

public enum SituacaoAceite {
    VIGENTE("Vigente"),
    /** Passou dos 12 meses. */
    VENCIDO("Vencido"),
    /** Um Gerente retirou o aceite. */
    REVOGADO("Revogado"),
    /** O risco mudou depois do aceite. */
    INVALIDADO("Invalidado");

    private final String rotulo;

    SituacaoAceite(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
