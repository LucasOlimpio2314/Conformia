package io.github.lucasolimpio.conformia.risco.web;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.risco.domain.SituacaoRisco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
import io.github.lucasolimpio.conformia.risco.repository.VulnerabilidadeRepository;
import io.github.lucasolimpio.conformia.risco.service.RiscoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Integração da tela de riscos: aplicação inteira + conformia_test (V3 com catálogos). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RiscoWebIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AtivoRepository ativoRepository;
    @Autowired private AmeacaRepository ameacaRepository;
    @Autowired private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Autowired private RiscoRepository riscoRepository;
    @Autowired private RiscoService riscoService;

    private Usuario analista;
    private UsuarioLogado analistaLogado;
    private UsuarioLogado auditorLogado;
    private Ativo ativo;
    private Long ameacaId;
    private Long vulnerabilidadeId;

    @BeforeEach
    void setUp() {
        analista = salvarUsuario("Rafael Risco", "rafael.risco@empresa.com", Perfil.ANALISTA);
        Usuario auditor = salvarUsuario("Beatriz Risco", "beatriz.risco@empresa.com", Perfil.AUDITOR);
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        auditorLogado = UsuarioLogado.de(auditor, Instant.now());
        ativo = ativoRepository.save(new Ativo("ATV-RIT", new DadosAtivo("ERP Risco IT", TipoAtivo.SOFTWARE, null, null,
                analista, false, null, Classificacao.CONFIDENCIAL, 4, 5, 3), analista, Instant.now()));
        ameacaId = ameacaRepository.findAllByOrderByNomeAsc().getFirst().getId();
        vulnerabilidadeId = vulnerabilidadeRepository.findAllByOrderByNomeAsc().getFirst().getId();
    }

    private Usuario salvarUsuario(String nome, String email, Perfil perfil) {
        Usuario u = new Usuario(nome, email, perfil, true, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        return usuarioRepository.save(u);
    }

    private RiscoResumo salvarRisco() {
        return riscoService.criar(new RiscoForm("Ransomware no ERP IT", null, List.of(ativo.getId()), ameacaId,
                vulnerabilidadeId, List.of("8.7", "8.13"), analista.getId(), 4, 5, null), analista.getId());
    }

    /** POST do drawer como o navegador envia (inclui marcadores "_" dos checkboxes). */
    private MockHttpServletRequestBuilder postRisco(String... controles) {
        return post("/riscos").with(user(analistaLogado)).with(csrf())
                .param("titulo", "Vazamento da base do ERP")
                .param("ativoIds", String.valueOf(ativo.getId())).param("_ativoIds", "on")
                .param("ameacaId", String.valueOf(ameacaId))
                .param("vulnerabilidadeId", String.valueOf(vulnerabilidadeId))
                .param("controles", controles).param("_controles", "on")
                .param("donoId", String.valueOf(analista.getId()))
                .param("probabilidade", "3").param("impacto", "5")
                .param("tratamento", "MITIGAR");
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void deveMostrarRiscosParaAuditorSemBotaoDeCadastro() throws Exception {
        // Arrange
        salvarRisco();

        // Act
        MvcResult resultado = mvc.perform(get("/riscos").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("Ransomware no ERP IT", "Modo leitura").doesNotContain("+ Novo risco");
    }

    @Test
    void deveAbrirFormularioComOs93ControlesDoAnexoA() throws Exception {
        // Arrange — catálogo carregado pela V3

        // Act
        MvcResult resultado = mvc.perform(get("/riscos/novo/form").with(user(analistaLogado))).andReturn();

        // Assert
        String html = corpo(resultado);
        int checkboxesDeControle = html.split("name=\"controles\"", -1).length - 1;
        assertThat(checkboxesDeControle).isEqualTo(93);
        assertThat(html).contains("Backup de informações", "ERP Risco IT");
    }

    @Test
    void deveCadastrarRiscoEMandarRecarregarALista() throws Exception {
        // Arrange — formulário válido com dois controles

        // Act
        MvcResult resultado = mvc.perform(postRisco("8.13", "5.23")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).startsWith("/riscos?salvo=RSK-");
        assertThat(riscoRepository.listarCompleto())
                .anyMatch(r -> r.getTitulo().equals("Vazamento da base do ERP") && r.getControles().size() == 2);
    }

    @Test
    void deveDevolverFormularioComErroQuandoNenhumControleForEscolhido() throws Exception {
        // Arrange — sem controles

        // Act
        MvcResult resultado = mvc.perform(postRisco()).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Escolha pelo menos um controle do Anexo A.");
    }

    @Test
    void deveNegarCadastroParaAuditor() throws Exception {
        // Arrange — auditor logado

        // Act
        MvcResult resultado = mvc.perform(post("/riscos").with(user(auditorLogado)).with(csrf())
                .param("titulo", "Tentativa do auditor")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveEncerrarRiscoComJustificativa() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco();

        // Act
        MvcResult resultado = mvc.perform(post("/riscos/" + risco.id() + "/encerrar").with(user(analistaLogado))
                .with(csrf()).param("justificativa", "ERP substituído, cenário deixou de existir")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).isEqualTo("/riscos?encerrado=" + risco.codigo());
        assertThat(riscoRepository.findById(risco.id()).orElseThrow().getSituacao()).isEqualTo(SituacaoRisco.ENCERRADO);
    }

    @Test
    void deveAvisarNaListaQuandoUmAtivoDoRiscoFoiDescontinuado() throws Exception {
        // Arrange — risco criado e depois o ativo descontinuado
        salvarRisco();
        ativo.descontinuar("ERP desligado em 10/11/2026", analista, Instant.now());

        // Act
        MvcResult resultado = mvc.perform(get("/riscos").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(corpo(resultado)).contains("Ativo descontinuado");
    }

    @Test
    void deveCadastrarNovaAmeacaPeloBotaoNova() throws Exception {
        // Arrange — nome que não existe no catálogo

        // Act
        MvcResult resultado = mvc.perform(post("/riscos/catalogo/ameacas").with(user(analistaLogado)).with(csrf())
                .param("nome", "Sabotagem de equipamento")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"nome\":\"Sabotagem de equipamento\"");
    }

    @Test
    void deveRecusarAmeacaRepetidaEmJson() throws Exception {
        // Arrange — "Erro humano" já vem na V3

        // Act
        MvcResult resultado = mvc.perform(post("/riscos/catalogo/ameacas").with(user(analistaLogado)).with(csrf())
                .param("nome", "ERRO HUMANO")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Esta ameaça já existe no catálogo.");
    }
}
