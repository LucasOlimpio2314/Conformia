package io.github.lucasolimpio.conformia.aceite.domain;

public enum TipoAceite {
    /** Risco com tratamento "Aceitar": aceita o nível inerente. */
    RISCO("Risco"),
    /** Depois do plano de ação: aceita o nível residual esperado. */
    RESIDUAL("Residual");

    private final String rotulo;

    TipoAceite(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
