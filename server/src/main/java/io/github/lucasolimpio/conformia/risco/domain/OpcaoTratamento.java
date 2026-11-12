package io.github.lucasolimpio.conformia.risco.domain;

/** Opções de tratamento (ISO 27001 6.1.3). No risco, null = "a definir". */
public enum OpcaoTratamento {

    MITIGAR("Mitigar", "Reduzir com controles (vira plano de ação)."),
    ACEITAR("Aceitar", "Conviver com o risco (exige aceite formal do Gerente)."),
    TRANSFERIR("Transferir", "Passar a terceiro: seguro, contrato, fornecedor."),
    EVITAR("Evitar", "Eliminar a atividade ou o ativo que gera o risco.");

    private final String rotulo;
    private final String descricao;

    OpcaoTratamento(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
