package io.github.lucasolimpio.conformia.risco.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Faixas da matriz 5x5. @ParameterizedTest roda o MESMO comportamento com
 * vários valores (as bordas de cada faixa) — continua sendo um comportamento por teste.
 */
class NivelRiscoTest {

    @ParameterizedTest(name = "valor {0} → {1}")
    @CsvSource({"1, BAIXO", "4, BAIXO", "5, MEDIO", "9, MEDIO", "10, ALTO", "16, ALTO", "17, CRITICO", "25, CRITICO"})
    void deveClassificarOValorNaFaixaCorreta(int valor, NivelRisco esperado) {
        // Arrange — valor e nível esperado vêm do @CsvSource

        // Act
        NivelRisco nivel = NivelRisco.de(valor);

        // Assert
        assertThat(nivel).isEqualTo(esperado);
    }

    @Test
    void deveRecusarValorForaDaMatriz() {
        // Arrange — a matriz 5x5 vai de 1 a 25

        // Act
        Throwable erro = catchThrowable(() -> NivelRisco.de(26));

        // Assert
        assertThat(erro).isInstanceOf(IllegalArgumentException.class);
    }
}
