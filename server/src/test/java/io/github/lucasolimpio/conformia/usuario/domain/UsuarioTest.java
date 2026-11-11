package io.github.lucasolimpio.conformia.usuario.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    void deveConsiderarSenhaTemporariaExpiradaDepoisDoPrazo() {
        // Arrange
        Usuario usuario = new Usuario("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true,
                "hash", AGORA.plus(Duration.ofHours(24)), AGORA);

        // Act
        boolean expirada = usuario.isSenhaExpirada(AGORA.plus(Duration.ofHours(25)));

        // Assert
        assertThat(expirada).isTrue();
    }

    @Test
    void naoDeveExpirarSenhaTemporariaDentroDoPrazo() {
        // Arrange
        Usuario usuario = new Usuario("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true,
                "hash", AGORA.plus(Duration.ofHours(24)), AGORA);

        // Act
        boolean expirada = usuario.isSenhaExpirada(AGORA.plus(Duration.ofHours(23)));

        // Assert
        assertThat(expirada).isFalse();
    }

    @Test
    void naoDeveExpirarSenhaTemporariaSemPrazo() {
        // Arrange — caso do usuário inicial criado pela migration
        Usuario usuario = new Usuario("Administrador", "admin@conformia.local", Perfil.GERENTE, true,
                "hash", null, AGORA);

        // Act
        boolean expirada = usuario.isSenhaExpirada(AGORA.plus(Duration.ofDays(365)));

        // Assert
        assertThat(expirada).isFalse();
    }

    @Test
    void naoDeveExpirarSenhaPessoal() {
        // Arrange
        Usuario usuario = new Usuario("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true,
                "hash", AGORA.plus(Duration.ofHours(24)), AGORA);
        usuario.definirSenhaPessoal("hash-pessoal");

        // Act
        boolean expirada = usuario.isSenhaExpirada(AGORA.plus(Duration.ofDays(30)));

        // Assert
        assertThat(expirada).isFalse();
    }
}
