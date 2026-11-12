package io.github.lucasolimpio.conformia.aviso.service;

import io.github.lucasolimpio.conformia.aviso.domain.Aviso;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.repository.AvisoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");

    @Mock private AvisoRepository repository;
    @Mock private UsuarioRepository usuarioRepository;

    private AvisoService service;

    @BeforeEach
    void setUp() {
        service = new AvisoService(repository, usuarioRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static Usuario gerente(String nome) {
        return new Usuario(nome, nome.toLowerCase() + "@empresa.com", Perfil.GERENTE, true, "hash", null, AGORA);
    }

    @Test
    void deveCriarUmAvisoParaCadaGerenteAtivo() {
        // Arrange — dois gerentes ativos
        when(usuarioRepository.findByAtivoTrueAndPerfilInOrderByNomeAsc(List.of(Perfil.GERENTE)))
                .thenReturn(List.of(gerente("Ana"), gerente("Mariana")));

        // Act
        service.notificarGerentes(TipoAviso.ACEITE_INVALIDADO, "Aceite do RSK-001 caiu", "Risco alterado", "/aceites");

        // Assert
        verify(repository, times(2)).save(any(Aviso.class));
    }

    @Test
    void deveMarcarComoLidoEDevolverOLinkAoAbrir() {
        // Arrange
        Aviso aviso = new Aviso(gerente("Ana"), TipoAviso.RESIDUAL_PRONTO, "Pronto", "Plano concluído", "/aceites", AGORA);
        when(repository.findByIdAndDestinatarioId(5L, 1L)).thenReturn(Optional.of(aviso));

        // Act
        String link = service.abrir(5L, 1L);

        // Assert
        assertThat(link).isEqualTo("/aceites");
        assertThat(aviso.isLido()).isTrue();
    }

    @Test
    void naoDeveAbrirAvisoDeOutroUsuario() {
        // Arrange — o aviso 5 não é do usuário 9
        when(repository.findByIdAndDestinatarioId(5L, 9L)).thenReturn(Optional.empty());

        // Act
        Throwable erro = catchThrowable(() -> service.abrir(5L, 9L));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage("Aviso não encontrado.");
    }
}
