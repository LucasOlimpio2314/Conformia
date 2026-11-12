package io.github.lucasolimpio.conformia.risco.service;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.controle.repository.ControleAnexoARepository;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import io.github.lucasolimpio.conformia.risco.domain.DadosRisco;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.domain.SituacaoRisco;
import io.github.lucasolimpio.conformia.risco.domain.Vulnerabilidade;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
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
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitário: sem Spring e sem banco. Entidades reais; ids atribuídos com
 * ReflectionTestUtils (no sistema quem atribui é o banco).
 */
@ExtendWith(MockitoExtension.class)
class RiscoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Long GERENTE_ID = 1L, ANALISTA_ID = 2L, AUDITOR_ID = 3L, INATIVO_ID = 4L;
    private static final Long ATIVO_ID = 10L, DESCONTINUADO_ID = 11L, RISCO_ID = 50L;

    @Mock private RiscoRepository repository;
    @Mock private AtivoRepository ativoRepository;
    @Mock private AmeacaRepository ameacaRepository;
    @Mock private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Mock private ControleAnexoARepository controleRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private RiscoService service;
    private Usuario gerente;
    private Usuario analista;
    private Ativo erp;
    private Ativo servidorAntigo;
    private ControleAnexoA backup;

    @BeforeEach
    void setUp() {
        service = new RiscoService(repository, ativoRepository, ameacaRepository, vulnerabilidadeRepository,
                controleRepository, usuarioRepository, Clock.fixed(AGORA, ZoneOffset.UTC));

        gerente = usuario(GERENTE_ID, "Mariana Costa", Perfil.GERENTE, true);
        analista = usuario(ANALISTA_ID, "Rafael Nunes", Perfil.ANALISTA, true);
        Usuario auditor = usuario(AUDITOR_ID, "Beatriz Lima", Perfil.AUDITOR, true);
        Usuario inativo = usuario(INATIVO_ID, "Tiago Moreira", Perfil.ANALISTA, false);

        erp = ativo(ATIVO_ID, "ATV-001", "ERP Financeiro");
        servidorAntigo = ativo(DESCONTINUADO_ID, "ATV-002", "Servidor antigo");
        servidorAntigo.descontinuar("Servidor desligado", gerente, AGORA);
        backup = controle("8.13", "Backup de informações");

        lenient().when(usuarioRepository.findById(GERENTE_ID)).thenReturn(Optional.of(gerente));
        lenient().when(usuarioRepository.findById(ANALISTA_ID)).thenReturn(Optional.of(analista));
        lenient().when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(auditor));
        lenient().when(usuarioRepository.findById(INATIVO_ID)).thenReturn(Optional.of(inativo));
        lenient().when(ativoRepository.findAllById(List.of(ATIVO_ID))).thenReturn(List.of(erp));
        lenient().when(ativoRepository.findAllById(List.of(DESCONTINUADO_ID))).thenReturn(List.of(servidorAntigo));
        lenient().when(controleRepository.findAllById(List.of("8.13"))).thenReturn(List.of(backup));
        lenient().when(ameacaRepository.findById(1L)).thenReturn(Optional.of(new Ameaca("Ransomware / malware", null, AGORA)));
        lenient().when(vulnerabilidadeRepository.findById(1L)).thenReturn(Optional.of(new Vulnerabilidade("Software sem atualização", null, AGORA)));
        lenient().when(repository.proximoNumeroCodigo()).thenReturn(7L);
        lenient().when(repository.save(any(Risco.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Usuario usuario(Long id, String nome, Perfil perfil, boolean ativo) {
        Usuario u = new Usuario(nome, id + "@empresa.com", perfil, ativo, "hash", null, AGORA);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Ativo ativo(Long id, String codigo, String nome) {
        Ativo a = new Ativo(codigo, new DadosAtivo(nome, TipoAtivo.SOFTWARE, null, null, analista, false, null,
                Classificacao.CONFIDENCIAL, 4, 5, 4), analista, AGORA);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    /** ControleAnexoA só tem construtor protegido (o sistema nunca cria: vem da migration). */
    private static ControleAnexoA controle(String codigo, String nome) {
        ControleAnexoA c = BeanUtils.instantiateClass(ControleAnexoA.class);
        ReflectionTestUtils.setField(c, "codigo", codigo);
        ReflectionTestUtils.setField(c, "nome", nome);
        ReflectionTestUtils.setField(c, "tema", 8);
        ReflectionTestUtils.setField(c, "ordem", 13);
        return c;
    }

    /** Formulário válido; cada teste muda só o que interessa. */
    private static RiscoForm form(Long ativoId, Long donoId, int probabilidade, int impacto) {
        return new RiscoForm("Sequestro de dados do ERP", "  ", List.of(ativoId), 1L, 1L, List.of("8.13"),
                donoId, probabilidade, impacto, OpcaoTratamento.MITIGAR);
    }

    private Risco riscoExistente() {
        Risco risco = repository.save(new Risco("RSK-001",
                new DadosRisco("Risco antigo", null,
                        new HashSet<>(List.of(servidorAntigo)), new Ameaca("Erro humano", null, AGORA),
                        new Vulnerabilidade("Configuração incorreta", null, AGORA),
                        new HashSet<>(List.of(backup)), gerente, 2, 3, null),
                gerente, AGORA.minusSeconds(3600)));
        lenient().when(repository.findById(RISCO_ID)).thenReturn(Optional.of(risco));
        return risco;
    }

    // ---------- cadastrar ----------

    @Test
    void deveGerarCodigoComPrefixoRSKESequenciaDoBanco() {
        // Arrange — sequência devolve 7

        // Act
        RiscoResumo criado = service.criar(form(ATIVO_ID, GERENTE_ID, 3, 3), ANALISTA_ID);

        // Assert
        assertThat(criado.codigo()).isEqualTo("RSK-007");
    }

    @Test
    void deveCalcularONivelPelaMatriz5x5() {
        // Arrange — probabilidade 4 × impacto 5 = 20

        // Act
        RiscoResumo criado = service.criar(form(ATIVO_ID, GERENTE_ID, 4, 5), ANALISTA_ID);

        // Assert
        assertThat(criado.valor()).isEqualTo(20);
        assertThat(criado.nivel()).isEqualTo(NivelRisco.CRITICO);
    }

    @Test
    void deveNascerAtivoEGuardarCenarioEmBrancoComoNulo() {
        // Arrange — cenário "  " no formulário

        // Act
        RiscoResumo criado = service.criar(form(ATIVO_ID, GERENTE_ID, 3, 3), ANALISTA_ID);

        // Assert
        assertThat(criado.situacao()).isEqualTo(SituacaoRisco.ATIVO);
        assertThat(criado.cenario()).isNull();
    }

    @Test
    void deveImpedirAuditorDeCadastrarRisco() {
        // Arrange — auditor é somente leitura

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(ATIVO_ID, GERENTE_ID, 3, 3), AUDITOR_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("O perfil Auditor tem acesso somente de leitura.");
        verify(repository, never()).save(any());
    }

    // ---------- ativos e controles ----------

    @Test
    void deveRecusarAtivoDescontinuadoEmRiscoNovo() {
        // Arrange — ATV-002 está descontinuado

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(DESCONTINUADO_ID, GERENTE_ID, 3, 3), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ATV-002 está descontinuado")
                .extracting("campo").isEqualTo("ativoIds");
    }

    @Test
    void devePermitirManterAtivoDescontinuadoQueJaEstavaNoRisco() {
        // Arrange — risco já ligado ao ATV-002, que foi descontinuado depois
        Risco risco = riscoExistente();

        // Act
        service.atualizar(RISCO_ID, form(DESCONTINUADO_ID, GERENTE_ID, 2, 2), ANALISTA_ID);

        // Assert
        assertThat(risco.getAtivos()).containsExactly(servidorAntigo);
        assertThat(risco.getAtualizadoPor()).isSameAs(analista);
    }

    @Test
    void deveRecusarControleQueNaoExisteNoAnexoA() {
        // Arrange — "9.99" não existe
        when(controleRepository.findAllById(List.of("9.99"))).thenReturn(List.of());
        RiscoForm comControleInvalido = new RiscoForm("Sequestro de dados do ERP", null, List.of(ATIVO_ID), 1L, 1L,
                List.of("9.99"), GERENTE_ID, 3, 3, null);

        // Act
        Throwable erro = catchThrowable(() -> service.criar(comControleInvalido, ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("controles");
    }

    // ---------- dono ----------

    @Test
    void deveRecusarAuditorComoDonoDoRisco() {
        // Arrange — dono = auditor

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(ATIVO_ID, AUDITOR_ID, 3, 3), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Auditor não pode ser dono de risco.");
    }

    @Test
    void deveRecusarDonoInativo() {
        // Arrange — dono desativado

        // Act
        Throwable erro = catchThrowable(() -> service.criar(form(ATIVO_ID, INATIVO_ID, 3, 3), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("donoId");
    }

    // ---------- encerrar ----------

    @Test
    void deveEncerrarGuardandoJustificativaEData() {
        // Arrange
        Risco risco = riscoExistente();

        // Act
        service.encerrar(RISCO_ID, "  Ativo descontinuado, cenário deixou de existir ", ANALISTA_ID);

        // Assert
        assertThat(risco.getSituacao()).isEqualTo(SituacaoRisco.ENCERRADO);
        assertThat(risco.getJustificativaEncerramento()).isEqualTo("Ativo descontinuado, cenário deixou de existir");
        assertThat(risco.getEncerradoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveExigirJustificativaComPeloMenos10Caracteres() {
        // Arrange
        riscoExistente();

        // Act
        Throwable erro = catchThrowable(() -> service.encerrar(RISCO_ID, "acabou", ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("justificativa");
    }

    @Test
    void deveImpedirEdicaoDeRiscoEncerrado() {
        // Arrange
        Risco risco = riscoExistente();
        risco.encerrar("Cenário deixou de existir", gerente, AGORA);

        // Act
        Throwable erro = catchThrowable(() -> service.atualizar(RISCO_ID, form(ATIVO_ID, GERENTE_ID, 3, 3), ANALISTA_ID));

        // Assert
        assertThat(erro).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Risco encerrado não pode ser alterado.");
    }

    // ---------- cards ----------

    @Test
    void deveContarPorNivelSoOsRiscosAtivos() {
        // Arrange — um crítico ativo, um crítico encerrado e um baixo ativo
        lenient().when(ativoRepository.findAllById(anyList())).thenReturn(List.of(erp));
        RiscoResumo critico = service.criar(form(ATIVO_ID, GERENTE_ID, 5, 5), ANALISTA_ID);
        RiscoResumo baixo = service.criar(form(ATIVO_ID, GERENTE_ID, 1, 2), ANALISTA_ID);
        Risco encerrado = riscoExistente();
        ReflectionTestUtils.setField(encerrado, "probabilidade", 5);
        ReflectionTestUtils.setField(encerrado, "impacto", 5);
        encerrado.encerrar("Cenário deixou de existir", gerente, AGORA);

        // Act
        Map<NivelRisco, Long> contagem = service.contarAtivosPorNivel(
                List.of(critico, baixo, RiscoResumo.de(encerrado)));

        // Assert
        assertThat(contagem).containsEntry(NivelRisco.CRITICO, 1L).containsEntry(NivelRisco.BAIXO, 1L)
                .containsEntry(NivelRisco.ALTO, 0L).containsEntry(NivelRisco.MEDIO, 0L);
    }
}
