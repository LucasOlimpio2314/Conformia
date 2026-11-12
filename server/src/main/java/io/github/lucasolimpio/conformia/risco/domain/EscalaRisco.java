package io.github.lucasolimpio.conformia.risco.domain;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Nomes das notas 1–5 da matriz. No banco só a nota (int);
 * o nome é para a tela entender o que cada número significa.
 */
public final class EscalaRisco {

    private static final String[] PROBABILIDADE = {"Rara", "Improvável", "Possível", "Provável", "Quase certa"};
    private static final String[] IMPACTO = {"Insignificante", "Menor", "Moderado", "Maior", "Catastrófico"};

    private EscalaRisco() {
    }

    public record Nota(int valor, String rotulo) {
    }

    public static List<Nota> probabilidades() {
        return IntStream.rangeClosed(1, 5).mapToObj(n -> new Nota(n, PROBABILIDADE[n - 1])).toList();
    }

    public static List<Nota> impactos() {
        return IntStream.rangeClosed(1, 5).mapToObj(n -> new Nota(n, IMPACTO[n - 1])).toList();
    }

    public static String rotuloProbabilidade(int nota) {
        return PROBABILIDADE[nota - 1];
    }

    public static String rotuloImpacto(int nota) {
        return IMPACTO[nota - 1];
    }
}
