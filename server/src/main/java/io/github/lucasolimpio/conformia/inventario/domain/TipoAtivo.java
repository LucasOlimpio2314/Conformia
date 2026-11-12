package io.github.lucasolimpio.conformia.inventario.domain;

/** Tipos de ativo (informação e ativos associados, ISO 27001 A.5.9). Gravado pelo nome. */
public enum TipoAtivo {

    INFORMACAO("Informação"),
    PROCESSO("Processo"),
    SOFTWARE("Software"),
    HARDWARE("Hardware"),
    REDE("Rede"),
    SERVICO_TERCEIRO("Serviço de terceiro"),
    PESSOAS("Pessoas"),
    INSTALACAO("Instalação");

    private final String rotulo;

    TipoAtivo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
