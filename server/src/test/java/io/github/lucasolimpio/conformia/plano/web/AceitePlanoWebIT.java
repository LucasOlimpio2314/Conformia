package io.github.lucasolimpio.conformia.plano.web;

import io.github.lucasolimpio.conformia.aceite.domain.SituacaoAceite;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteForm;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.repository.AceiteRiscoRepository;
import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
import io.github.lucasolimpio.conformia.aviso.domain.Aviso;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.repository.AvisoRepository;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.plano.dto.AcaoForm;
import io.github.lucasolimpio.conformia.plano.dto.AcaoResumo;
import io.github.lucasolimpio.conformia.plano.dto.ResidualForm;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.plano.service.PlanoAcaoService;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.VulnerabilidadeRepository;
import io.github.lucasolimpio.conformia.risco.service.RiscoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integração do plano de ação, aceite formal e avisos pela tela web
 * (aplicação inteira + conformia_test, transação desfeita no final).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AceitePlanoWebIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AtivoRepository ativoRepository;
    @Autowired private AmeacaRepository ameacaRepository;
    @Autowired private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Autowired private RiscoService riscoService;
    @Autowired private PlanoAcaoService planoService;
    @Autowired private AceiteService aceiteService;
    @Autowired private AceiteRiscoRepository aceiteRepository;
    @Autowired private AvisoRepository avisoRepository;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    private Usuario gerente;
    private Usuario analista;
    private UsuarioLogado gerenteLogado;
    private UsuarioLogado analistaLogado;
    private UsuarioLogado auditorLogado;
    private Ativo ativo;

    @BeforeEach
    void setUp() {
        gerente = usuarioRepository.findByEmail(adminEmail.toLowerCase(Locale.ROOT)).orElseThrow();
        analista = salvarUsuario("Rafael Plano", "rafael.plano@empresa.com", Perfil.ANALISTA);
        gerenteLogado = UsuarioLogado.de(gerente, Instant.now());
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        auditorLogado = UsuarioLogado.de(salvarUsuario("Beatriz Plano", "beatriz.plano@empresa.com", Perfil.AUDITOR), Instant.now());
        ativo = ativoRepository.save(new Ativo("ATV-PIT", new DadosAtivo("ERP Plano IT", TipoAtivo.SOFTWARE, null, null,
                analista, false, null, Classificacao.CONFIDENCIAL, 4, 5, 3), analista, Instant.now()));
    }

    private Usuario salvarUsuario(String nome, String email, Perfil perfil) {
        Usuario u = new Usuario(nome, email, perfil, true, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        return usuarioRepository.save(u);
    }

    /** Risco P4 × I5 = 20 com o tratamento pedido. */
    private RiscoResumo salvarRisco(OpcaoTratamento tratamento, int impacto) {
        return riscoService.criar(new RiscoForm("Ransomware no ERP Plano IT", null, List.of(ativo.getId()),
                ameacaRepository.findAllByOrderByNomeAsc().getFirst().getId(),
                vulnerabilidadeRepository.findAllByOrderByNomeAsc().getFirst().getId(),
                List.of("8.13", "8.7"), analista.getId(), 4, impacto, tratamento), analista.getId());
    }

    private AcaoResumo salvarAcao(Long riscoId) {
        return planoService.criarAcao(riscoId, new AcaoForm("Backup imutável diário", analista.getId(),
                LocalDate.now().plusDays(30), "8.13", SituacaoAcao.PENDENTE, null), analista.getId());
    }

    private List<Aviso> avisosDoGerente(TipoAviso tipo) {
        return avisoRepository.findTop100ByDestinatarioIdOrderByCriadoEmDesc(gerente.getId()).stream()
                .filter(a -> a.getTipo() == tipo).toList();
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------- plano de ação ----------

    @Test
    void deveMostrarOPlanoParaAuditorEmModoLeitura() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR, 5);
        salvarAcao(risco.id());

        // Act
        MvcResult resultado = mvc.perform(get("/plano").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains(risco.codigo(), "Backup imutável diário", "Modo leitura")
                .doesNotContain("+ Ação");
    }

    @Test
    void deveCriarAcaoPeloDrawer() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR, 5);

        // Act
        MvcResult resultado = mvc.perform(post("/plano/" + risco.id() + "/acoes").with(user(analistaLogado)).with(csrf())
                .param("titulo", "Ativar MFA no ERP")
                .param("responsavelId", String.valueOf(analista.getId()))
                .param("prazo", LocalDate.now().plusDays(10).toString())
                .param("controle", "8.7")
                .param("situacao", "PENDENTE")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).startsWith("/plano?salvo=ACT-");
    }

    @Test
    void deveDevolverFormularioComErroAoConcluirSemEvidencia() throws Exception {
        // Arrange
        AcaoResumo acao = salvarAcao(salvarRisco(OpcaoTratamento.MITIGAR, 5).id());

        // Act
        MvcResult resultado = mvc.perform(post("/plano/acoes/" + acao.id()).with(user(analistaLogado)).with(csrf())
                .param("titulo", acao.titulo())
                .param("responsavelId", String.valueOf(analista.getId()))
                .param("prazo", acao.prazo().toString())
                .param("controle", "8.13")
                .param("situacao", "CONCLUIDA")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Informe a evidência para concluir a ação");
    }

    @Test
    void deveNegarAlteracaoDoPlanoParaAuditor() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR, 5);

        // Act
        MvcResult resultado = mvc.perform(post("/plano/" + risco.id() + "/residual").with(user(auditorLogado)).with(csrf())
                .param("probabilidade", "2").param("impacto", "2")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveLevarOResidualParaAceiteEAvisarQuandoOPlanoTermina() throws Exception {
        // Arrange — residual 2×3 e uma ação em andamento
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR, 5);
        AcaoResumo acao = salvarAcao(risco.id());
        planoService.definirResidual(risco.id(), new ResidualForm(2, 3), analista.getId());

        // Act — concluir a última ação pela tela
        mvc.perform(post("/plano/acoes/" + acao.id()).with(user(analistaLogado)).with(csrf())
                .param("titulo", acao.titulo())
                .param("responsavelId", String.valueOf(analista.getId()))
                .param("prazo", acao.prazo().toString())
                .param("controle", "8.13")
                .param("situacao", "CONCLUIDA")
                .param("evidencia", "Chamado #4512 — backup imutável ativo"));

        // Assert
        assertThat(avisosDoGerente(TipoAviso.RESIDUAL_PRONTO)).anyMatch(a -> a.getTitulo().startsWith(risco.codigo()));
        String aceites = corpo(mvc.perform(get("/aceites").with(user(gerenteLogado))).andReturn());
        assertThat(aceites).contains(risco.codigo(), "Risco residual · após plano de ação", "2×3=6");
    }

    // ---------- aceite formal ----------

    @Test
    void deveRegistrarOAceitePeloDrawer() throws Exception {
        // Arrange — risco com tratamento Aceitar
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);

        // Act
        MvcResult resultado = mvc.perform(post("/aceites/" + risco.id()).with(user(gerenteLogado)).with(csrf())
                .param("justificativa", "Impacto baixo e contingência manual testada.")
                .param("declaracao", "true").param("_declaracao", "on")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).isEqualTo("/aceites?aceito=" + risco.codigo());
        assertThat(aceiteRepository.findByRiscoIdAndSituacao(risco.id(), SituacaoAceite.VIGENTE)).isPresent();
    }

    @Test
    void deveDevolverErroQuandoADeclaracaoNaoForMarcada() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);

        // Act
        MvcResult resultado = mvc.perform(post("/aceites/" + risco.id()).with(user(gerenteLogado)).with(csrf())
                .param("justificativa", "Impacto baixo e contingência manual testada.")
                .param("_declaracao", "on")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Marque a declaração de aceite.");
    }

    @Test
    void deveNegarAceiteParaAnalista() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);

        // Act
        MvcResult resultado = mvc.perform(post("/aceites/" + risco.id()).with(user(analistaLogado)).with(csrf())
                .param("justificativa", "Tentativa do analista de aceitar.").param("declaracao", "true")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveDerrubarOAceiteEAvisarQuandoORiscoMuda() throws Exception {
        // Arrange — risco aceito (P4 × I2)
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);
        AceiteResumo aceite = aceiteService.aceitar(risco.id(),
                new AceiteForm("Impacto baixo e contingência manual testada.", null, true), gerente.getId());

        // Act — analista sobe o impacto para 3 pela tela
        mvc.perform(post("/riscos/" + risco.id()).with(user(analistaLogado)).with(csrf())
                .param("titulo", risco.titulo())
                .param("ativoIds", String.valueOf(ativo.getId())).param("_ativoIds", "on")
                .param("ameacaId", String.valueOf(risco.ameaca().id()))
                .param("vulnerabilidadeId", String.valueOf(risco.vulnerabilidade().id()))
                .param("controles", "8.13", "8.7").param("_controles", "on")
                .param("donoId", String.valueOf(analista.getId()))
                .param("probabilidade", "4").param("impacto", "3").param("tratamento", "ACEITAR"));

        // Assert
        assertThat(aceiteRepository.findById(aceite.id()).orElseThrow().getSituacao()).isEqualTo(SituacaoAceite.INVALIDADO);
        assertThat(avisosDoGerente(TipoAviso.ACEITE_INVALIDADO))
                .anyMatch(a -> a.getMensagem().contains("impacto 2 → 3") && a.getMensagem().contains("Rafael Plano"));
    }

    @Test
    void deveRevogarOAceiteComMotivo() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);
        AceiteResumo aceite = aceiteService.aceitar(risco.id(),
                new AceiteForm("Impacto baixo e contingência manual testada.", null, true), gerente.getId());

        // Act
        MvcResult resultado = mvc.perform(post("/aceites/revogacoes/" + aceite.id()).with(user(gerenteLogado)).with(csrf())
                .param("justificativa", "O sistema passou a ter vendas online")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).isEqualTo("/aceites?revogado=" + risco.codigo());
        assertThat(aceiteRepository.findById(aceite.id()).orElseThrow().getSituacao()).isEqualTo(SituacaoAceite.REVOGADO);
    }

    // ---------- avisos e menu ----------

    @Test
    void deveAbrirOAvisoMarcandoComoLidoELevandoParaATela() throws Exception {
        // Arrange — aceite derrubado por mudança de impacto gera um aviso para o gerente
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR, 2);
        aceiteService.aceitar(risco.id(), new AceiteForm("Impacto baixo e contingência manual testada.", null, true), gerente.getId());
        riscoService.atualizar(risco.id(), new RiscoForm(risco.titulo(), null, List.of(ativo.getId()), risco.ameaca().id(),
                risco.vulnerabilidade().id(), List.of("8.13", "8.7"), analista.getId(), 4, 3, OpcaoTratamento.ACEITAR), analista.getId());
        Aviso aviso = avisosDoGerente(TipoAviso.ACEITE_INVALIDADO).getFirst();

        // Act
        MvcResult resultado = mvc.perform(post("/avisos/" + aviso.getId() + "/abrir").with(user(gerenteLogado)).with(csrf())).andReturn();

        // Assert
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/aceites");
        assertThat(avisoRepository.findById(aviso.getId()).orElseThrow().isLido()).isTrue();
    }

    @Test
    void deveMostrarContadorDeAceitesPendentesNoMenu() throws Exception {
        // Arrange — um risco "Aceitar" sem aceite
        salvarRisco(OpcaoTratamento.ACEITAR, 2);

        // Act
        MvcResult resultado = mvc.perform(get("/riscos").with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(corpo(resultado)).contains("title=\"Aguardando aceite\"", "aguardando aceite");
    }
}
