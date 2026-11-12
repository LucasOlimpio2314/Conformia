package io.github.lucasolimpio.conformia.inventario.domain;

/**
 * Escala 1–5 usada nas notas C-I-D e na criticidade do ativo.
 * É a mesma escala do impacto na matriz de risco 5x5.
 */
public enum Criticidade {

    MUITO_BAIXA(1, "Muito baixa"),
    BAIXA(2, "Baixa"),
    MEDIA(3, "Média"),
    ALTA(4, "Alta"),
    MUITO_ALTA(5, "Muito alta");

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;

    private final int nota;
    private final String rotulo;

    Criticidade(int nota, String rotulo) {
        this.nota = nota;
        this.rotulo = rotulo;
    }

    public static Criticidade daNota(int nota) {
        if (nota < MINIMO || nota > MAXIMO) {
            throw new IllegalArgumentException("Nota fora da escala 1–5: " + nota);
        }
        return values()[nota - 1];
    }

    public int getNota() {
        return nota;
    }

    public String getRotulo() {
        return rotulo;
    }
}
