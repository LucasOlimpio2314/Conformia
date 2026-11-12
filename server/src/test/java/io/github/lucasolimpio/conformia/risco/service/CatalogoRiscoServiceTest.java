package io.github.lucasolimpio.conformia.risco.service;

import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import io.github.lucasolimpio.conformia.risco.dto.ItemCatalogo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.VulnerabilidadeRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoRiscoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");

    @Mock private AmeacaRepository ameacaRepository;
    @Mock private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private CatalogoRiscoService service;

    @BeforeEach
    void setUp() {
        service = new CatalogoRiscoService(ameacaRepository, vulnerabilidadeRepository, usuarioRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        lenient().when(usuarioRepository.findById(2L)).thenReturn(Optional.of(
                new Usuario("Rafael Nunes", "rafael@empresa.com", Perfil.ANALISTA, true, "hash", null, AGORA)));
        lenient().when(usuarioRepository.findById(3L)).thenReturn(Optional.of(
                new Usuario("Beatriz Lima", "beatriz@empresa.com", Perfil.AUDITOR, true, "hash", null, AGORA)));
        lenient().when(ameacaRepository.save(any(Ameaca.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void deveCadastrarAmeacaJuntandoEspacosRepetidos() {
        // Arrange — nome com espaços sobrando

        // Act
        ItemCatalogo item = service.cadastrarAmeaca("  Vazamento   por   e-mail  ", 2L);

        // Assert
        assertThat(item.nome()).isEqualTo("Vazamento por e-mail");
    }

    @Test
    void deveRecusarAmeacaQueJaExisteIgnorandoMaiusculas() {
        // Arrange
        when(ameacaRepository.existsByNomeIgnoreCase("phishing e engenharia social")).thenReturn(true);

        // Act
        Throwable erro = catchThrowable(() -> service.cadastrarAmeaca("phishing e engenharia social", 2L));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Esta ameaça já existe no catálogo.");
        verify(ameacaRepository, never()).save(any());
    }

    @Test
    void deveRecusarNomeComMenosDe3Caracteres() {
        // Arrange — nome curto

        // Act
        Throwable erro = catchThrowable(() -> service.cadastrarVulnerabilidade("ab", 2L));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("nome");
    }

    @Test
    void deveImpedirAuditorDeAmpliarOCatalogo() {
        // Arrange — auditor é somente leitura

        // Act
        Throwable erro = catchThrowable(() -> service.cadastrarAmeaca("Sabotagem", 3L));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("O perfil Auditor tem acesso somente de leitura.");
    }
}
