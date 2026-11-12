package io.github.lucasolimpio.conformia.inventario.domain;

/** Classificação da informação (ISO 27001 A.5.12), da menos à mais sensível. */
public enum Classificacao {

    PUBLICA("Pública", "Pode ser divulgada sem dano (site, material de marketing)."),
    INTERNA("Interna", "Uso dos colaboradores; vazamento causa dano pequeno."),
    CONFIDENCIAL("Confidencial", "Acesso por necessidade; vazamento causa dano relevante (dados de clientes, contratos)."),
    RESTRITA("Restrita", "Pouquíssimas pessoas; vazamento causa dano grave ou legal (credenciais, dados sensíveis).");

    private final String rotulo;
    private final String descricao;

    Classificacao(String rotulo, String descricao) {
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
