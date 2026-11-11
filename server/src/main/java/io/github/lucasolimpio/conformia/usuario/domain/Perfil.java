package io.github.lucasolimpio.conformia.usuario.domain;

/**
 * Perfil de acesso. Gravado no banco pelo nome (EnumType.STRING), então
 * renomear uma constante exige migration.
 */
public enum Perfil {

    GERENTE("Gerente", "Tudo do analista, mais aceite formal de risco e gestão de usuários."),
    ANALISTA("Analista", "Registra e trata riscos, mantém inventário e plano de ação."),
    AUDITOR("Auditor", "Somente leitura, para auditoria interna ou externa.");

    private final String rotulo;
    private final String descricao;

    Perfil(String rotulo, String descricao) {
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
