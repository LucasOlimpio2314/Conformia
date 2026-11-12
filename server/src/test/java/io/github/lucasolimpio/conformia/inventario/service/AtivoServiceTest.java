package io.github.lucasolimpio.conformia.inventario.service;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.SituacaoAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoForm;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoResumo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/** Unitário: sem Spring e sem banco. Repositórios são mocks; o relógio é fixo. */
@ExtendWith(MockitoExtension.class)
class AtivoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Long GERENTE_ID = 1L;
    private static final Long ANALISTA_ID = 2L;
    private static final Long AUDITOR_ID = 3L;
    private static final Long INATIVO_ID = 4L;
    private static final Long ATIVO_ID = 10L;

    @Mock
    private AtivoRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private AtivoService service;
    private Usuario gerente;
    private Usuario analista;

    @BeforeEach
    void setUp() {
        service = new AtivoService(repository, usuarioRepository, Clock.fixed(AGORA, ZoneOffset.UTC));

        gerente = usuario("Mariana Costa", Perfil.GERENTE, true);
        analista = usuario("Rafael Nunes", Perfil.ANALISTA, true);
        Usuario auditor = usuario("Beatriz Lima", Perfil.AUDITOR, true);
        Usuario inativo = usuario("Tiago Moreira", Perfil.ANALISTA, false);

        lenient().when(usuarioRepository.findById(GERENTE_ID)).thenReturn(Optional.of(gerente));
        lenient().when(usuarioRepository.findById(ANALISTA_ID)).thenReturn(Optional.of(analista));
        lenient().when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(auditor));
        lenient().when(usuarioRepository.findById(INATIVO_ID)).thenReturn(Optional.of(inativo));
        lenient().when(repository.proximoNumeroCodigo()).thenReturn(7L);
        lenient().when(repository.save(any(Ativo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Usuario usuario(String nome, Perfil perfil, boolean ativo) {
        return new Usuario(nome, nome.toLowerCase().replace(' ', '.') + "@empresa.com", perfil, ativo, "hash", null, AGORA);
    }

    /** Formulário válido; cada teste muda só o que interessa. */
    private static AtivoForm form(Long proprietarioId, boolean terceirizado, String fornecedor) {
        return new AtivoForm("ERP Financeiro", TipoAtivo.SOFTWARE, "  ", "sa-east-1", proprietarioId,
                terceirizado, fornecedor, Classificacao.CONFIDENCIAL, 4, 5, 3);
    }

    private Ativo ativoExistente() {
        DadosAtivo dados = new DadosAtivo("Notebooks", TipoAtivo.HARDWARE, null, null, analista, false, null,
                Classificacao.INTERNA, 3, 2, 3);
        Ativo ativo = new Ativo("ATV-001", dados, gerente, AGORA.minusSeconds(3600));
        lenient().when(repository.findById(ATIVO_ID)).thenReturn(Optional.of(ativo));
        return ativo;
    }

    // ---------- cadastrar ----------

    @Test
    void deveGerarCodigoComPrefixoATVESequenciaDoBanco() {
        // Arrange — sequência devolve 7

        // Act
        AtivoResumo criado = service.criar(form(GERENTE_ID, false, null), ANALISTA_ID);

        // Assert
        assertThat(criado.codigo()).isEqualTo("ATV-007");
    }

    @Test
    void deveCalcularCriticidadePelaMaiorNotaCID() {
        // Arrange — C4 I5 D3

        // Act
        AtivoResumo criado = service.criar(form(GERENTE_ID, false, null), ANALISTA_ID);

        // Assert
        assertThat(criado.criticidade().getNota()).isEqualTo(5);
    }

    @Test
    void deveRegistrarQuemCadastrouEQuando() {
        // Arrange — analista cadastrando

        // Act
        service.criar(form(GERENTE_ID, false, null), ANALISTA_ID);

        // Assert
        ArgumentCaptor<Ativo> salvo = ArgumentCaptor.forClass(Ativo.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getCriadoPor()).isSameAs(analista);
        assertThat(salvo.getValue().getCriadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveGuardarTextoEmBrancoComoNulo() {
        // Arrange — descrição "  " no formulário

        // Act
        AtivoResumo criado = service.criar(form(GERENTE_ID, false, null), ANALISTA_ID);

        // Assert
        assertThat(criado.descricao()).isNull();
    }

    @Test
    void deveImpedirAuditorDeCadastrarAtivo() {
        // Arrange — auditor é somente leitura

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(GERENTE_ID, false, null), AUDITOR_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("O perfil Auditor tem acesso somente de leitura.");
        verify(repository, never()).save(any());
    }

    // ---------- proprietário ----------

    @Test
    void deveRecusarAuditorComoProprietario() {
        // Arrange — proprietário escolhido é auditor

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(AUDITOR_ID, false, null), GERENTE_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Auditor não pode ser proprietário de ativo.")
                .extracting("campo").isEqualTo("proprietarioId");
    }

    @Test
    void deveRecusarProprietarioInativo() {
        // Arrange — proprietário escolhido está desativado

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(INATIVO_ID, false, null), GERENTE_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo")
                .extracting("campo").isEqualTo("proprietarioId");
    }

    @Test
    void deveDeixarProprietarioVazioNaEdicaoQuandoOAtualEstiverInativo() {
        // Arrange — ativo cujo proprietário foi desativado depois
        Usuario saiu = usuario("Tiago Moreira", Perfil.ANALISTA, false);
        Ativo ativo = new Ativo("ATV-002", new DadosAtivo("Backup", TipoAtivo.SERVICO_TERCEIRO, null, null, saiu,
                true, "Backblaze", Classificacao.RESTRITA, 5, 4, 4), gerente, AGORA);
        when(repository.findById(ATIVO_ID)).thenReturn(Optional.of(ativo));

        // Act
        AtivoForm edicao = service.formularioDeEdicao(ATIVO_ID);

        // Assert
        assertThat(edicao.proprietarioId()).isNull();
    }

    // ---------- terceirizado ----------

    @Test
    void deveExigirFornecedorQuandoAtivoForTerceirizado() {
        // Arrange — terceirizado sem fornecedor

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(GERENTE_ID, true, " "), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("fornecedor");
    }

    @Test
    void deveGuardarFornecedorQuandoAtivoForTerceirizado() {
        // Arrange
        AtivoForm terceirizado = form(GERENTE_ID, true, " AWS ");

        // Act
        AtivoResumo criado = service.criar(terceirizado, ANALISTA_ID);

        // Assert
        assertThat(criado.terceirizado()).isTrue();
        assertThat(criado.fornecedor()).isEqualTo("AWS");
    }

    @Test
    void deveDescartarFornecedorQuandoAtivoNaoForTerceirizado() {
        // Arrange — fornecedor preenchido, mas chave desligada

        // Act
        AtivoResumo criado = service.criar(form(GERENTE_ID, false, "AWS"), ANALISTA_ID);

        // Assert
        assertThat(criado.fornecedor()).isNull();
    }

    // ---------- editar ----------

    @Test
    void deveAtualizarERegistrarQuemAlterou() {
        // Arrange
        Ativo ativo = ativoExistente();

        // Act
        service.atualizar(ATIVO_ID, form(GERENTE_ID, false, null), ANALISTA_ID);

        // Assert
        assertThat(ativo.getNome()).isEqualTo("ERP Financeiro");
        assertThat(ativo.getAtualizadoPor()).isSameAs(analista);
        assertThat(ativo.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(ativo.getCodigo()).isEqualTo("ATV-001");
    }

    @Test
    void deveImpedirEdicaoDeAtivoDescontinuado() {
        // Arrange
        Ativo ativo = ativoExistente();
        ativo.descontinuar("Equipamentos doados em 10/11/2026", gerente, AGORA);

        // Act
        Throwable erro = catchThrowable(() -> service.atualizar(ATIVO_ID, form(GERENTE_ID, false, null), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Ativo descontinuado não pode ser alterado.");
    }

    // ---------- descontinuar ----------

    @Test
    void deveDescontinuarGuardandoJustificativaEData() {
        // Arrange
        Ativo ativo = ativoExistente();

        // Act
        service.descontinuar(ATIVO_ID, "  Equipamentos doados em 10/11/2026 ", ANALISTA_ID);

        // Assert
        assertThat(ativo.getSituacao()).isEqualTo(SituacaoAtivo.DESCONTINUADO);
        assertThat(ativo.getJustificativaDescontinuacao()).isEqualTo("Equipamentos doados em 10/11/2026");
        assertThat(ativo.getDescontinuadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveExigirJustificativaComPeloMenos10Caracteres() {
        // Arrange
        ativoExistente();

        // Act
        Throwable erro = catchThrowable(() -> service.descontinuar(ATIVO_ID, "doado", ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("justificativa");
    }

    @Test
    void deveImpedirAuditorDeDescontinuarAtivo() {
        // Arrange
        ativoExistente();

        // Act
        Throwable erro = catchThrowable(() -> service.descontinuar(ATIVO_ID, "Equipamentos doados", AUDITOR_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("O perfil Auditor tem acesso somente de leitura.");
    }
}
