package io.github.lucasolimpio.conformia.usuario.service;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.dto.NovoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.SenhaTemporariaGerada;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitário: sem Spring e sem banco. Repository, encoder e gerador são mocks;
 * o relógio é fixo para testar o prazo da senha temporária.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Long GERENTE_ID = 1L;
    private static final Long ANALISTA_ID = 2L;

    @Mock
    private UsuarioRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GeradorSenha geradorSenha;

    private UsuarioService service;
    private Usuario gerente;
    private Usuario analista;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder, geradorSenha, Clock.fixed(AGORA, ZoneOffset.UTC));

        gerente = new Usuario("Ana Gerente", "ana@empresa.com", Perfil.GERENTE, true, "hash-ana", null, AGORA);
        gerente.definirSenhaPessoal("hash-ana");
        analista = new Usuario("Rafael Nunes", "rafael@empresa.com", Perfil.ANALISTA, true, "hash-rafael", null, AGORA);
        analista.definirSenhaPessoal("hash-rafael");

        lenient().when(repository.findById(GERENTE_ID)).thenReturn(Optional.of(gerente));
        lenient().when(repository.findById(ANALISTA_ID)).thenReturn(Optional.of(analista));
        lenient().when(geradorSenha.gerar()).thenReturn("Temp2345ab");
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
    }

    // ---------- criar ----------

    @Test
    void deveCriarUsuarioComSenhaTemporariaQueExpiraEm24Horas() {
        // Arrange
        NovoUsuarioForm form = new NovoUsuarioForm("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true);

        // Act
        service.criar(form, GERENTE_ID);

        // Assert
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().isSenhaTemporaria()).isTrue();
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash:Temp2345ab");
        assertThat(salvo.getValue().getSenhaExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(24)));
    }

    @Test
    void deveDevolverSenhaTemporariaEmTextoAbertoSoNaCriacao() {
        // Arrange
        NovoUsuarioForm form = new NovoUsuarioForm("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true);

        // Act
        SenhaTemporariaGerada gerada = service.criar(form, GERENTE_ID);

        // Assert
        assertThat(gerada.senha()).isEqualTo("Temp2345ab");
        assertThat(gerada.nomeUsuario()).isEqualTo("Juliana Prado");
    }

    @Test
    void deveGuardarEmailEmMinusculasESemEspacos() {
        // Arrange
        NovoUsuarioForm form = new NovoUsuarioForm("Juliana Prado", "  Juliana@Empresa.COM ", Perfil.ANALISTA, true);

        // Act
        service.criar(form, GERENTE_ID);

        // Assert
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("juliana@empresa.com");
    }

    @Test
    void deveImpedirCadastroComEmailJaUsado() {
        // Arrange
        when(repository.existsByEmail("rafael@empresa.com")).thenReturn(true);
        NovoUsuarioForm form = new NovoUsuarioForm("Outro Rafael", "rafael@empresa.com", Perfil.AUDITOR, true);

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form, GERENTE_ID));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um usuário com este e-mail.")
                .extracting("campo").isEqualTo("email");
        verify(repository, never()).save(any());
    }

    @Test
    void deveImpedirQueAnalistaCrieUsuario() {
        // Arrange
        NovoUsuarioForm form = new NovoUsuarioForm("Juliana Prado", "juliana@empresa.com", Perfil.ANALISTA, true);

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form, ANALISTA_ID));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só um Gerente pode gerenciar usuários.");
    }

    // ---------- redefinir senha (gerente para outro usuário) ----------

    @Test
    void deveGerarNovaSenhaTemporariaAoRedefinirSenhaDeOutroUsuario() {
        // Arrange — analista já tem senha pessoal

        // Act
        SenhaTemporariaGerada gerada = service.redefinirSenha(ANALISTA_ID, GERENTE_ID);

        // Assert
        assertThat(gerada.senha()).isEqualTo("Temp2345ab");
        assertThat(analista.isSenhaTemporaria()).isTrue();
        assertThat(analista.getSenhaHash()).isEqualTo("hash:Temp2345ab");
        assertThat(analista.getSenhaExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(24)));
    }

    @Test
    void deveImpedirGerenteDeRedefinirAPropriaSenhaComSenhaTemporaria() {
        // Arrange — gerente pedindo para si mesmo

        // Act
        Throwable erro = catchThrowable(() -> service.redefinirSenha(GERENTE_ID, GERENTE_ID));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Para alterar a sua própria senha, use Minha conta.");
    }

    @Test
    void deveImpedirAnalistaDeRedefinirSenhaDeOutroUsuario() {
        // Arrange — analista pedindo para o gerente

        // Act
        Throwable erro = catchThrowable(() -> service.redefinirSenha(GERENTE_ID, ANALISTA_ID));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só um Gerente pode gerenciar usuários.");
    }

    // ---------- troca obrigatória após senha temporária ----------

    @Test
    void deveTrocarSenhaTemporariaPorSenhaPessoalERegistrarAcesso() {
        // Arrange
        analista.definirSenhaTemporaria("hash-temp", AGORA.plus(Duration.ofHours(24)));
        TrocaSenhaForm form = new TrocaSenhaForm(null, "MinhaSenha9", "MinhaSenha9");

        // Act
        service.trocarSenhaTemporaria(ANALISTA_ID, form);

        // Assert
        assertThat(analista.isSenhaTemporaria()).isFalse();
        assertThat(analista.getSenhaHash()).isEqualTo("hash:MinhaSenha9");
        assertThat(analista.getSenhaExpiraEm()).isNull();
        assertThat(analista.getUltimoAcesso()).isEqualTo(AGORA);
    }

    @Test
    void deveRecusarTrocaQuandoSenhaJaFoiDefinida() {
        // Arrange — analista já tem senha pessoal
        TrocaSenhaForm form = new TrocaSenhaForm(null, "MinhaSenha9", "MinhaSenha9");

        // Act
        Throwable erro = catchThrowable(() -> service.trocarSenhaTemporaria(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi definida");
    }

    @Test
    void deveRecusarNovaSenhaComMenosDe8Caracteres() {
        // Arrange
        analista.definirSenhaTemporaria("hash-temp", null);
        TrocaSenhaForm form = new TrocaSenhaForm(null, "abc123", "abc123");

        // Act
        Throwable erro = catchThrowable(() -> service.trocarSenhaTemporaria(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("novaSenha");
    }

    @Test
    void deveRecusarNovaSenhaSemNumero() {
        // Arrange
        analista.definirSenhaTemporaria("hash-temp", null);
        TrocaSenhaForm form = new TrocaSenhaForm(null, "somenteletras", "somenteletras");

        // Act
        Throwable erro = catchThrowable(() -> service.trocarSenhaTemporaria(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Use letras e números na nova senha.");
    }

    @Test
    void deveRecusarQuandoConfirmacaoForDiferente() {
        // Arrange
        analista.definirSenhaTemporaria("hash-temp", null);
        TrocaSenhaForm form = new TrocaSenhaForm(null, "MinhaSenha9", "MinhaSenha8");

        // Act
        Throwable erro = catchThrowable(() -> service.trocarSenhaTemporaria(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("confirmacao");
    }

    @Test
    void deveRecusarNovaSenhaIgualATemporaria() {
        // Arrange
        analista.definirSenhaTemporaria("hash-temp", null);
        when(passwordEncoder.matches("Temp2345ab", "hash-temp")).thenReturn(true);
        TrocaSenhaForm form = new TrocaSenhaForm(null, "Temp2345ab", "Temp2345ab");

        // Act
        Throwable erro = catchThrowable(() -> service.trocarSenhaTemporaria(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A nova senha precisa ser diferente da atual.");
    }

    // ---------- minha conta ----------

    @Test
    void deveAlterarPropriaSenhaQuandoSenhaAtualConfere() {
        // Arrange
        when(passwordEncoder.matches("SenhaAtual1", "hash-rafael")).thenReturn(true);
        TrocaSenhaForm form = new TrocaSenhaForm("SenhaAtual1", "NovaSenha22", "NovaSenha22");

        // Act
        service.alterarPropriaSenha(ANALISTA_ID, form);

        // Assert
        assertThat(analista.getSenhaHash()).isEqualTo("hash:NovaSenha22");
        assertThat(analista.isSenhaTemporaria()).isFalse();
    }

    @Test
    void deveRecusarAlteracaoQuandoSenhaAtualNaoConfere() {
        // Arrange
        when(passwordEncoder.matches("errada", "hash-rafael")).thenReturn(false);
        TrocaSenhaForm form = new TrocaSenhaForm("errada", "NovaSenha22", "NovaSenha22");

        // Act
        Throwable erro = catchThrowable(() -> service.alterarPropriaSenha(ANALISTA_ID, form));

        // Assert
        assertThat(erro)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Senha atual incorreta.")
                .extracting("campo").isEqualTo("senhaAtual");
    }
}
