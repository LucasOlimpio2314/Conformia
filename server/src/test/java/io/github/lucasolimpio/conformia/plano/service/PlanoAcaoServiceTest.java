package io.github.lucasolimpio.conformia.plano.service;

import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.DadosAcao;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.plano.dto.AcaoForm;
import io.github.lucasolimpio.conformia.plano.dto.AcaoResumo;
import io.github.lucasolimpio.conformia.plano.dto.ResidualForm;
import io.github.lucasolimpio.conformia.plano.repository.AcaoPlanoRepository;
import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import io.github.lucasolimpio.conformia.risco.domain.DadosRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.domain.Vulnerabilidade;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unitário: sem Spring e sem banco. AceiteService e AvisoService são mocks. */
@ExtendWith(MockitoExtension.class)
class PlanoAcaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final LocalDate PRAZO = LocalDate.of(2026, 12, 15);
    private static final Long ANALISTA_ID = 2L, AUDITOR_ID = 3L, RISCO_ID = 50L, ACAO_ID = 70L;

    @Mock private AcaoPlanoRepository repository;
    @Mock private RiscoRepository riscoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AceiteService aceiteService;
    @Mock private AvisoService avisoService;

    private PlanoAcaoService service;
    private Usuario analista;
    private ControleAnexoA backup;
    private Risco risco;

    @BeforeEach
    void setUp() {
        service = new PlanoAcaoService(repository, riscoRepository, usuarioRepository, aceiteService, avisoService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        analista = usuario(ANALISTA_ID, Perfil.ANALISTA);
        Usuario auditor = usuario(AUDITOR_ID, Perfil.AUDITOR);
        backup = BeanUtils.instantiateClass(ControleAnexoA.class);
        ReflectionTestUtils.setField(backup, "codigo", "8.13");
        ReflectionTestUtils.setField(backup, "nome", "Backup de informações");
        ReflectionTestUtils.setField(backup, "tema", 8);
        risco = risco(OpcaoTratamento.MITIGAR);

        lenient().when(usuarioRepository.findById(ANALISTA_ID)).thenReturn(Optional.of(analista));
        lenient().when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(auditor));
        lenient().when(riscoRepository.findById(RISCO_ID)).thenReturn(Optional.of(risco));
        lenient().when(repository.proximoNumeroCodigo()).thenReturn(4L);
        lenient().when(repository.save(any(AcaoPlano.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Usuario usuario(Long id, Perfil perfil) {
        Usuario u = new Usuario("Usuário " + id, id + "@empresa.com", perfil, true, "hash", null, AGORA);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    /** Risco P3 × I4 = 12 ligado só ao controle 8.13. */
    private Risco risco(OpcaoTratamento tratamento) {
        Ativo ativo = new Ativo("ATV-001", new DadosAtivo("ERP", TipoAtivo.SOFTWARE, null, null, analista, false, null,
                Classificacao.INTERNA, 3, 3, 3), analista, AGORA);
        Risco r = new Risco("RSK-001", new DadosRisco("Ransomware no ERP", null, new HashSet<>(List.of(ativo)),
                new Ameaca("Ransomware", null, AGORA), new Vulnerabilidade("Sem patch", null, AGORA),
                new HashSet<>(List.of(backup)), analista, 3, 4, tratamento), analista, AGORA);
        ReflectionTestUtils.setField(r, "id", RISCO_ID);
        return r;
    }

    private static AcaoForm form(Long responsavelId, String controle, SituacaoAcao situacao, String evidencia) {
        return new AcaoForm("Backup imutável diário", responsavelId, PRAZO, controle, situacao, evidencia);
    }

    private AcaoPlano acaoExistente(SituacaoAcao situacao) {
        AcaoPlano acao = new AcaoPlano("ACT-001", risco, new DadosAcao("Backup imutável diário", analista, PRAZO,
                backup, situacao, situacao == SituacaoAcao.CONCLUIDA ? "Chamado #1" : null), analista, AGORA);
        lenient().when(repository.findById(ACAO_ID)).thenReturn(Optional.of(acao));
        lenient().when(repository.findByRiscoId(RISCO_ID)).thenReturn(List.of(acao));
        return acao;
    }

    // ---------- ações ----------

    @Test
    void deveCriarAcaoPendenteComCodigoACT() {
        // Arrange — sequência devolve 4

        // Act
        AcaoResumo acao = service.criarAcao(RISCO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.PENDENTE, null), ANALISTA_ID);

        // Assert
        assertThat(acao.codigo()).isEqualTo("ACT-004");
        assertThat(acao.situacao()).isEqualTo(SituacaoAcao.PENDENTE);
    }

    @Test
    void deveRecusarControleQueNaoEstaNoRisco() {
        // Arrange — 5.15 existe na norma, mas não está ligado ao risco

        // Act
        Throwable erro = catchThrowable(() ->
                service.criarAcao(RISCO_ID, form(ANALISTA_ID, "5.15", SituacaoAcao.PENDENTE, null), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("controle");
    }

    @Test
    void deveExigirEvidenciaParaConcluir() {
        // Arrange — concluir sem evidência
        acaoExistente(SituacaoAcao.EM_ANDAMENTO);

        // Act
        Throwable erro = catchThrowable(() ->
                service.atualizarAcao(ACAO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.CONCLUIDA, "  "), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("evidencia");
    }

    @Test
    void naoDeveConsumirCodigoDaSequenciaQuandoAValidacaoFalha() {
        // Arrange — concluir sem evidência é recusado

        // Act
        catchThrowable(() -> service.criarAcao(RISCO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.CONCLUIDA, null), ANALISTA_ID));

        // Assert
        verify(repository, never()).proximoNumeroCodigo();
    }

    @Test
    void deveRecusarAuditorComoResponsavel() {
        // Arrange — responsável = auditor

        // Act
        Throwable erro = catchThrowable(() ->
                service.criarAcao(RISCO_ID, form(AUDITOR_ID, "8.13", SituacaoAcao.PENDENTE, null), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Auditor não pode ser responsável por ação.");
    }

    @Test
    void deveImpedirAlterarAcaoConcluida() {
        // Arrange
        acaoExistente(SituacaoAcao.CONCLUIDA);

        // Act
        Throwable erro = catchThrowable(() ->
                service.atualizarAcao(ACAO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.PENDENTE, null), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Ação concluída não pode ser alterada.");
    }

    @Test
    void deveRecusarPlanoParaRiscoComTratamentoAceitar() {
        // Arrange — risco "Aceitar" vai direto para o aceite, sem plano
        when(riscoRepository.findById(RISCO_ID)).thenReturn(Optional.of(risco(OpcaoTratamento.ACEITAR)));

        // Act
        Throwable erro = catchThrowable(() ->
                service.criarAcao(RISCO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.PENDENTE, null), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Mitigar, Transferir ou Evitar");
    }

    @Test
    void deveDerrubarAceiteDoResidualQuandoUmaAcaoNovaEIncluida() {
        // Arrange — plano reaberto por uma ação nova

        // Act
        service.criarAcao(RISCO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.PENDENTE, null), ANALISTA_ID);

        // Assert
        verify(aceiteService).invalidarSeVigente(eq(risco), eq(analista), contains("ACT-004 incluída"), eq(true));
    }

    @Test
    void deveAvisarOsGerentesQuandoAUltimaAcaoForConcluida() {
        // Arrange — residual definido e a única ação em andamento
        risco.definirResidual(2, 2, analista, AGORA);
        acaoExistente(SituacaoAcao.EM_ANDAMENTO);

        // Act
        service.atualizarAcao(ACAO_ID, form(ANALISTA_ID, "8.13", SituacaoAcao.CONCLUIDA, "Chamado #4512"), ANALISTA_ID);

        // Assert
        verify(avisoService).notificarGerentes(eq(TipoAviso.RESIDUAL_PRONTO),
                eq("RSK-001 pronto para aceite do risco residual"), contains("2 × 2 = 4"), eq("/aceites"));
    }

    // ---------- residual ----------

    @Test
    void deveRecusarResidualMaiorQueOInerente() {
        // Arrange — inerente 3 × 4 = 12; residual 4 × 4 = 16

        // Act
        Throwable erro = catchThrowable(() -> service.definirResidual(RISCO_ID, new ResidualForm(4, 4), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser maior que o risco inerente");
    }

    @Test
    void deveDerrubarAceiteQuandoOResidualMuda() {
        // Arrange — residual 2×2 já definido
        risco.definirResidual(2, 2, analista, AGORA);

        // Act
        service.definirResidual(RISCO_ID, new ResidualForm(2, 3), ANALISTA_ID);

        // Assert
        verify(aceiteService).invalidarSeVigente(eq(risco), eq(analista), contains("2×2 → 2×3"), eq(true));
    }

    @Test
    void naoDeveDerrubarAceiteQuandoOResidualNaoMuda() {
        // Arrange
        risco.definirResidual(2, 2, analista, AGORA);

        // Act
        service.definirResidual(RISCO_ID, new ResidualForm(2, 2), ANALISTA_ID);

        // Assert
        verify(aceiteService, never()).invalidarSeVigente(any(), any(), anyString(), anyBoolean());
    }

    @Test
    void deveImpedirAuditorDeAlterarOPlano() {
        // Arrange — auditor é somente leitura

        // Act
        Throwable erro = catchThrowable(() -> service.definirResidual(RISCO_ID, new ResidualForm(2, 2), AUDITOR_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("O perfil Auditor tem acesso somente de leitura.");
    }
}
