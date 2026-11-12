package io.github.lucasolimpio.conformia.aceite.service;

import io.github.lucasolimpio.conformia.aceite.domain.AceiteRisco;
import io.github.lucasolimpio.conformia.aceite.domain.SituacaoAceite;
import io.github.lucasolimpio.conformia.aceite.domain.TipoAceite;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteForm;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.repository.AceiteRiscoRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unitário: sem Spring e sem banco. Relógio fixo em 11/11/2026 (UTC). */
@ExtendWith(MockitoExtension.class)
class AceiteServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Long GERENTE_ID = 1L, ANALISTA_ID = 2L, RISCO_ID = 50L;

    @Mock private AceiteRiscoRepository repository;
    @Mock private RiscoRepository riscoRepository;
    @Mock private AcaoPlanoRepository acaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AvisoService avisoService;

    private AceiteService service;
    private Usuario gerente;
    private Usuario analista;
    private ControleAnexoA backup;

    @BeforeEach
    void setUp() {
        service = new AceiteService(repository, riscoRepository, acaoRepository, usuarioRepository, avisoService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        gerente = usuario(GERENTE_ID, "Mariana Costa", Perfil.GERENTE);
        analista = usuario(ANALISTA_ID, "Rafael Nunes", Perfil.ANALISTA);
        backup = BeanUtils.instantiateClass(ControleAnexoA.class);
        ReflectionTestUtils.setField(backup, "codigo", "8.13");
        ReflectionTestUtils.setField(backup, "nome", "Backup de informações");
        ReflectionTestUtils.setField(backup, "tema", 8);

        lenient().when(usuarioRepository.findById(GERENTE_ID)).thenReturn(Optional.of(gerente));
        lenient().when(usuarioRepository.findById(ANALISTA_ID)).thenReturn(Optional.of(analista));
        lenient().when(repository.save(any(AceiteRisco.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Usuario usuario(Long id, String nome, Perfil perfil) {
        Usuario u = new Usuario(nome, id + "@empresa.com", perfil, true, "hash", null, AGORA);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    /** Risco P3 × I4 = 12 com o tratamento pedido, já "no banco" (stubs do repository). */
    private Risco risco(OpcaoTratamento tratamento) {
        Ativo ativo = new Ativo("ATV-001", new DadosAtivo("ERP", TipoAtivo.SOFTWARE, null, null, analista, false, null,
                Classificacao.INTERNA, 3, 3, 3), analista, AGORA);
        ReflectionTestUtils.setField(ativo, "id", 10L);
        Risco risco = new Risco("RSK-001", new DadosRisco("Ransomware no ERP", null, new HashSet<>(List.of(ativo)),
                new Ameaca("Ransomware", null, AGORA), new Vulnerabilidade("Sem patch", null, AGORA),
                new HashSet<>(List.of(backup)), gerente, 3, 4, tratamento), analista, AGORA);
        ReflectionTestUtils.setField(risco, "id", RISCO_ID);
        lenient().when(riscoRepository.listarCompleto()).thenReturn(List.of(risco));
        lenient().when(riscoRepository.getReferenceById(RISCO_ID)).thenReturn(risco);
        return risco;
    }

    private AcaoPlano acaoConcluida(Risco risco) {
        AcaoPlano acao = new AcaoPlano("ACT-001", risco, new DadosAcao("Aplicar patches", analista, LocalDate.of(2026, 11, 1),
                backup, SituacaoAcao.CONCLUIDA, "Chamado #4512"), analista, AGORA);
        when(acaoRepository.findAll()).thenReturn(List.of(acao));
        return acao;
    }

    private static AceiteForm formValido() {
        return new AceiteForm("Impacto aceitável: ERP tem contingência manual testada.", null, true);
    }

    // ---------- aceitar ----------

    @Test
    void deveAceitarRiscoComTratamentoAceitarGuardandoONivelInerente() {
        // Arrange
        risco(OpcaoTratamento.ACEITAR);

        // Act
        AceiteResumo aceite = service.aceitar(RISCO_ID, formValido(), GERENTE_ID);

        // Assert
        assertThat(aceite.tipo()).isEqualTo(TipoAceite.RISCO);
        assertThat(aceite.valor()).isEqualTo(12);
        assertThat(aceite.situacao()).isEqualTo(SituacaoAceite.VIGENTE);
    }

    @Test
    void deveValerPor12Meses() {
        // Arrange
        risco(OpcaoTratamento.ACEITAR);

        // Act
        AceiteResumo aceite = service.aceitar(RISCO_ID, formValido(), GERENTE_ID);

        // Assert
        assertThat(aceite.validoAte()).isEqualTo(Instant.parse("2027-11-11T12:00:00Z"));
    }

    @Test
    void deveAceitarOResidualQuandoOPlanoEstiverConcluido() {
        // Arrange — Mitigar, residual 2×2 e a única ação concluída
        Risco risco = risco(OpcaoTratamento.MITIGAR);
        risco.definirResidual(2, 2, analista, AGORA);
        acaoConcluida(risco);

        // Act
        AceiteResumo aceite = service.aceitar(RISCO_ID, formValido(), GERENTE_ID);

        // Assert
        assertThat(aceite.tipo()).isEqualTo(TipoAceite.RESIDUAL);
        assertThat(aceite.valor()).isEqualTo(4);
    }

    @Test
    void deveRecusarAceiteDeRiscoQueNaoEstaAguardando() {
        // Arrange — Mitigar sem plano concluído
        risco(OpcaoTratamento.MITIGAR);

        // Act
        Throwable erro = catchThrowable(() -> service.aceitar(RISCO_ID, formValido(), GERENTE_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Este risco não está aguardando aceite.");
    }

    @Test
    void deveImpedirAnalistaDeAceitar() {
        // Arrange
        risco(OpcaoTratamento.ACEITAR);

        // Act
        Throwable erro = catchThrowable(() -> service.aceitar(RISCO_ID, formValido(), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só um Gerente pode aceitar ou revogar aceites.");
        verify(repository, never()).save(any());
    }

    @Test
    void deveExigirADeclaracaoMarcada() {
        // Arrange
        risco(OpcaoTratamento.ACEITAR);
        AceiteForm semDeclaracao = new AceiteForm("Impacto aceitável: ERP tem contingência manual.", null, false);

        // Act
        Throwable erro = catchThrowable(() -> service.aceitar(RISCO_ID, semDeclaracao, GERENTE_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("declaracao");
    }

    // ---------- invalidar, vencer, revogar ----------

    @Test
    void deveInvalidarOAceiteVigenteEAvisarOsGerentes() {
        // Arrange
        Risco risco = risco(OpcaoTratamento.ACEITAR);
        AceiteRisco vigente = new AceiteRisco(risco, TipoAceite.RISCO, 3, 4, "justificativa", null, gerente, AGORA, AGORA.plusSeconds(1000));
        when(repository.findByRiscoIdAndSituacao(RISCO_ID, SituacaoAceite.VIGENTE)).thenReturn(Optional.of(vigente));

        // Act
        service.invalidarSeVigente(risco, analista, "Risco alterado (impacto 4 → 5)", true);

        // Assert
        assertThat(vigente.getSituacao()).isEqualTo(SituacaoAceite.INVALIDADO);
        assertThat(vigente.getMotivoEncerramento()).contains("impacto 4 → 5", "Rafael Nunes", "11/11/2026");
        verify(avisoService).notificarGerentes(eq(TipoAviso.ACEITE_INVALIDADO), eq("Aceite do RSK-001 caiu"),
                contains("impacto 4 → 5"), eq("/aceites"));
    }

    @Test
    void deveMarcarComoVencidoEAvisarQuandoPassarDos12Meses() {
        // Arrange — aceite que venceu ontem
        Risco risco = risco(OpcaoTratamento.ACEITAR);
        AceiteRisco antigo = new AceiteRisco(risco, TipoAceite.RISCO, 3, 4, "justificativa", null, gerente,
                AGORA.minusSeconds(366L * 24 * 3600), AGORA.minusSeconds(24 * 3600));
        when(repository.findBySituacaoAndValidoAteBefore(SituacaoAceite.VIGENTE, AGORA)).thenReturn(List.of(antigo));

        // Act
        int vencidos = service.atualizarVencidos();

        // Assert
        assertThat(vencidos).isEqualTo(1);
        assertThat(antigo.getSituacao()).isEqualTo(SituacaoAceite.VENCIDO);
        verify(avisoService).notificarGerentes(eq(TipoAviso.ACEITE_VENCIDO), eq("Aceite do RSK-001 venceu"),
                anyString(), eq("/aceites"));
    }

    @Test
    void deveRevogarComMotivo() {
        // Arrange
        Risco risco = risco(OpcaoTratamento.ACEITAR);
        AceiteRisco vigente = new AceiteRisco(risco, TipoAceite.RISCO, 3, 4, "justificativa", null, gerente, AGORA, AGORA.plusSeconds(1000));
        when(repository.findById(7L)).thenReturn(Optional.of(vigente));

        // Act
        service.revogar(7L, "O site passou a ter vendas online", GERENTE_ID);

        // Assert
        assertThat(vigente.getSituacao()).isEqualTo(SituacaoAceite.REVOGADO);
        assertThat(vigente.getMotivoEncerramento()).isEqualTo("O site passou a ter vendas online");
    }

    @Test
    void deveExigirMotivoParaRevogar() {
        // Arrange
        Risco risco = risco(OpcaoTratamento.ACEITAR);
        AceiteRisco vigente = new AceiteRisco(risco, TipoAceite.RISCO, 3, 4, "justificativa", null, gerente, AGORA, AGORA.plusSeconds(1000));
        when(repository.findById(7L)).thenReturn(Optional.of(vigente));

        // Act
        Throwable erro = catchThrowable(() -> service.revogar(7L, "não", GERENTE_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("justificativa");
    }
}
