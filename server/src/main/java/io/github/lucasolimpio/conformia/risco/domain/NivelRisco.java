package io.github.lucasolimpio.conformia.risco.domain;

/**
 * Faixas da matriz 5x5: nível = probabilidade × impacto (1 a 25).
 * Declarado do mais grave para o mais leve (ordem dos cards na tela).
 */
public enum NivelRisco {

    CRITICO("Crítico", 17, 25),
    ALTO("Alto", 10, 16),
    MEDIO("Médio", 5, 9),
    BAIXO("Baixo", 1, 4);

    private final String rotulo;
    private final int minimo;
    private final int maximo;

    NivelRisco(String rotulo, int minimo, int maximo) {
        this.rotulo = rotulo;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public static NivelRisco de(int valor) {
        for (NivelRisco nivel : values()) {
            if (valor >= nivel.minimo && valor <= nivel.maximo) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Valor fora da matriz 5x5: " + valor);
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getMinimo() {
        return minimo;
    }

    public int getMaximo() {
        return maximo;
    }

    /** Ex.: "17–25". */
    public String getFaixa() {
        return minimo + "–" + maximo;
    }
}
