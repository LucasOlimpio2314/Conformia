package io.github.lucasolimpio.conformia.risco.api;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Integração da API de riscos: aplicação inteira + conformia_test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RiscoApiIT {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AtivoRepository ativoRepository;
    @Autowired private AmeacaRepository ameacaRepository;
    @Autowired private VulnerabilidadeRepository vulnerabilidadeRepository;
    @Autowired private RiscoService riscoService;

    private Usuario analista;
    private UsuarioLogado analistaLogado;
    private UsuarioLogado auditorLogado;
    private Ativo ativo;
    private Long ameacaId;
    private Long vulnerabilidadeId;

    @BeforeEach
    void setUp() {
        analista = salvarUsuario("Rafael Api Risco", "rafael.apirisco@empresa.com", Perfil.ANALISTA);
        auditorLogado = UsuarioLogado.de(salvarUsuario("Beatriz Api Risco", "beatriz.apirisco@empresa.com",
                Perfil.AUDITOR), Instant.now());
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        ativo = ativoRepository.save(new Ativo("ATV-RAPI", new DadosAtivo("CRM Api Risco", TipoAtivo.INFORMACAO, null,
                null, analista, false, null, Classificacao.CONFIDENCIAL, 5, 4, 3), analista, Instant.now()));
        ameacaId = ameacaRepository.findAllByOrderByNomeAsc().getFirst().getId();
        vulnerabilidadeId = vulnerabilidadeRepository.findAllByOrderByNomeAsc().getFirst().getId();
    }

    private Usuario salvarUsuario(String nome, String email, Perfil perfil) {
        Usuario u = new Usuario(nome, email, perfil, true, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        return usuarioRepository.save(u);
    }

    private String json(String controles) {
        return "{\"titulo\":\"Vazamento do CRM\",\"ativoIds\":[" + ativo.getId() + "],\"ameacaId\":" + ameacaId
                + ",\"vulnerabilidadeId\":" + vulnerabilidadeId + ",\"controles\":[" + controles + "],\"donoId\":"
                + analista.getId() + ",\"probabilidade\":4,\"impacto\":5,\"tratamento\":\"MITIGAR\"}";
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void deveEntregarAsOpcoesDoFormularioComCatalogosEControles() throws Exception {
        // Arrange — catálogos da V3

        // Act
        MvcResult resultado = mvc.perform(get("/api/riscos/opcoes").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"codigo\":\"8.13\"", "\"nome\":\"Backup de informações\"",
                "Ransomware / malware", "\"codigo\":\"ATV-RAPI\"", "\"rotulo\":\"Quase certa\"");
    }

    @Test
    void deveCadastrarRiscoECalcularONivel() throws Exception {
        // Arrange — 4 × 5 = 20 (crítico)

        // Act
        MvcResult resultado = mvc.perform(post("/api/riscos").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json("\"8.13\",\"5.34\""))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"codigo\":\"RSK-", "\"valor\":20", "\"nivel\":\"CRITICO\"",
                "\"situacao\":\"ATIVO\"");
    }

    @Test
    void deveResponder400QuandoFaltarControle() throws Exception {
        // Arrange — "controles": []

        // Act
        MvcResult resultado = mvc.perform(post("/api/riscos").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json(""))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado)).contains("\"controles\"");
    }

    @Test
    void deveNegarCadastroParaAuditor() throws Exception {
        // Arrange — auditor logado

        // Act
        MvcResult resultado = mvc.perform(post("/api/riscos").with(user(auditorLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json("\"8.13\""))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveAtualizarRiscoPelaApi() throws Exception {
        // Arrange
        RiscoResumo risco = riscoService.criar(new RiscoForm("Risco API", null, List.of(ativo.getId()), ameacaId,
                vulnerabilidadeId, List.of("8.13"), analista.getId(), 1, 1, null), analista.getId());

        // Act
        MvcResult resultado = mvc.perform(put("/api/riscos/" + risco.id()).with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json("\"8.13\""))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"codigo\":\"" + risco.codigo() + "\"", "\"titulo\":\"Vazamento do CRM\"");
    }

    @Test
    void deveEncerrarRiscoPelaApi() throws Exception {
        // Arrange
        RiscoResumo risco = riscoService.criar(new RiscoForm("Risco API 2", null, List.of(ativo.getId()), ameacaId,
                vulnerabilidadeId, List.of("8.13"), analista.getId(), 2, 2, null), analista.getId());

        // Act
        MvcResult resultado = mvc.perform(post("/api/riscos/" + risco.id() + "/encerrar").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Cenário deixou de existir\"}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"situacao\":\"ENCERRADO\"");
    }

    @Test
    void deveCadastrarNovaVulnerabilidadePelaApi() throws Exception {
        // Arrange — nome novo

        // Act
        MvcResult resultado = mvc.perform(post("/api/riscos/catalogo/vulnerabilidades").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Firmware desatualizado\"}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"nome\":\"Firmware desatualizado\"");
    }
}
