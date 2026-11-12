package io.github.lucasolimpio.conformia.plano.domain;

public enum SituacaoAcao {
    PENDENTE("Pendente", false),
    EM_ANDAMENTO("Em andamento", false),
    /** Exige evidência. Ação finalizada não é mais editada. */
    CONCLUIDA("Concluída", true),
    /** Não conta no progresso do plano. */
    CANCELADA("Cancelada", true);

    private final String rotulo;
    private final boolean finalizada;

    SituacaoAcao(String rotulo, boolean finalizada) {
        this.rotulo = rotulo;
        this.finalizada = finalizada;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isFinalizada() {
        return finalizada;
    }
}
