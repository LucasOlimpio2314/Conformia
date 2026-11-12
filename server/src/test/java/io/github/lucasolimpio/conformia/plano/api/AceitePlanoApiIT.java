package io.github.lucasolimpio.conformia.plano.api;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
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
import org.springframework.http.MediaType;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Integração da API do plano de ação, aceites e avisos (app mobile). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AceitePlanoApiIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AtivoRepository ativoRepository;
    @Autowired private AmeacaRepository ameacaRepository;
    @Autowired private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Autowired private RiscoService riscoService;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    private Usuario analista;
    private UsuarioLogado gerenteLogado;
    private UsuarioLogado analistaLogado;
    private Ativo ativo;

    @BeforeEach
    void setUp() {
        gerenteLogado = UsuarioLogado.de(usuarioRepository.findByEmail(adminEmail.toLowerCase(Locale.ROOT)).orElseThrow(),
                Instant.now());
        Usuario u = new Usuario("Rafael Api Plano", "rafael.apiplano@empresa.com", Perfil.ANALISTA, true, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        analista = usuarioRepository.save(u);
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        ativo = ativoRepository.save(new Ativo("ATV-PAPI", new DadosAtivo("CRM Api Plano", TipoAtivo.INFORMACAO, null,
                null, analista, false, null, Classificacao.CONFIDENCIAL, 5, 4, 3), analista, Instant.now()));
    }

    private RiscoResumo salvarRisco(OpcaoTratamento tratamento) {
        return riscoService.criar(new RiscoForm("Vazamento do CRM Api Plano", null, List.of(ativo.getId()),
                ameacaRepository.findAllByOrderByNomeAsc().getFirst().getId(),
                vulnerabilidadeRepository.findAllByOrderByNomeAsc().getFirst().getId(),
                List.of("8.13"), analista.getId(), 3, 4, tratamento), analista.getId());
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void deveCriarAcaoPelaApi() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR);
        String json = "{\"titulo\":\"Ativar MFA no CRM\",\"responsavelId\":" + analista.getId() + ",\"prazo\":\""
                + LocalDate.now().plusDays(20) + "\",\"controle\":\"8.13\",\"situacao\":\"PENDENTE\"}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/plano/" + risco.id() + "/acoes").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ACT-", "\"situacao\":\"PENDENTE\"", "\"atrasada\":false");
    }

    @Test
    void deveDefinirOResidualPelaApi() throws Exception {
        // Arrange — inerente 3 × 4 = 12
        RiscoResumo risco = salvarRisco(OpcaoTratamento.MITIGAR);

        // Act
        MvcResult resultado = mvc.perform(put("/api/plano/" + risco.id() + "/residual").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content("{\"probabilidade\":2,\"impacto\":2}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"valorResidual\":4", "\"nivelResidual\":\"BAIXO\"");
    }

    @Test
    void deveListarPendenciasDeAceite() throws Exception {
        // Arrange — risco "Aceitar" sem aceite
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR);

        // Act
        MvcResult resultado = mvc.perform(get("/api/aceites/pendencias").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"codigo\":\"" + risco.codigo() + "\"", "\"tipo\":\"RISCO\"");
    }

    @Test
    void deveAceitarPelaApiComoGerente() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR);
        String json = "{\"justificativa\":\"Contingência manual testada e impacto controlado.\",\"declaracao\":true}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/aceites/riscos/" + risco.id()).with(user(gerenteLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"situacao\":\"VIGENTE\"", "\"tipo\":\"RISCO\"", "\"valor\":12");
    }

    @Test
    void deveNegarAceitePelaApiParaAnalista() throws Exception {
        // Arrange
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR);

        // Act
        MvcResult resultado = mvc.perform(post("/api/aceites/riscos/" + risco.id()).with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justificativa\":\"Tentativa do analista.\",\"declaracao\":true}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ACESSO_NEGADO\"");
    }

    @Test
    void deveExigirADeclaracaoNaApi() throws Exception {
        // Arrange — "declaracao" ausente
        RiscoResumo risco = salvarRisco(OpcaoTratamento.ACEITAR);

        // Act
        MvcResult resultado = mvc.perform(post("/api/aceites/riscos/" + risco.id()).with(user(gerenteLogado))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justificativa\":\"Contingência manual testada e impacto controlado.\"}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado)).contains("\"declaracao\"");
    }

    @Test
    void deveContarAvisosNaoLidos() throws Exception {
        // Arrange — gerente logado

        // Act
        MvcResult resultado = mvc.perform(get("/api/avisos/nao-lidos").with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"quantidade\":");
    }
}
