package io.github.lucasolimpio.conformia.usuario.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorSenhaTest {

    private final GeradorSenha gerador = new GeradorSenha();

    @Test
    void deveGerarSenhaCom10Caracteres() {
        // Arrange — gerador criado no campo

        // Act
        String senha = gerador.gerar();

        // Assert
        assertThat(senha).hasSize(10);
    }

    @Test
    void naoDeveUsarCaracteresAmbiguos() {
        // Arrange
        StringBuilder muitas = new StringBuilder();

        // Act — várias senhas para cobrir o alfabeto
        for (int i = 0; i < 500; i++) {
            muitas.append(gerador.gerar());
        }

        // Assert
        assertThat(muitas.toString()).doesNotContain("0", "O", "1", "l", "I");
    }
}
