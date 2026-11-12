package io.github.lucasolimpio.conformia.inventario.domain;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AtivoTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private final Usuario autor = new Usuario("Mariana Costa", "mariana@empresa.com", Perfil.GERENTE, true,
            "hash", null, AGORA);

    private Ativo ativo(int c, int i, int d) {
        DadosAtivo dados = new DadosAtivo("ERP", TipoAtivo.SOFTWARE, null, null, autor, false, null,
                Classificacao.INTERNA, c, i, d);
        return new Ativo("ATV-001", dados, autor, AGORA);
    }

    @Test
    void deveUsarAMaiorNotaEntreCIDComoCriticidade() {
        // Arrange
        Ativo ativo = ativo(2, 4, 3);

        // Act
        Criticidade criticidade = ativo.getCriticidade();

        // Assert
        assertThat(criticidade).isEqualTo(Criticidade.ALTA);
    }

    @Test
    void deveNascerEmUso() {
        // Arrange + Act — o construtor é a ação
        Ativo ativo = ativo(1, 1, 1);

        // Assert
        assertThat(ativo.isEmUso()).isTrue();
    }

    @Test
    void naoDeveDescontinuarDuasVezes() {
        // Arrange
        Ativo ativo = ativo(3, 3, 3);
        ativo.descontinuar("Servidor descartado", autor, AGORA);

        // Act
        Throwable erro = catchThrowable(() -> ativo.descontinuar("De novo", autor, AGORA));

        // Assert
        assertThat(erro).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRecusarNotaForaDaEscala() {
        // Arrange — escala vai de 1 a 5

        // Act
        Throwable erro = catchThrowable(() -> Criticidade.daNota(6));

        // Assert
        assertThat(erro).isInstanceOf(IllegalArgumentException.class);
    }
}
