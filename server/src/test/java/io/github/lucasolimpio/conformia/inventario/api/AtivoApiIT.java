package io.github.lucasolimpio.conformia.inventario.api;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Integração da API do inventário: aplicação inteira + conformia_test, transação desfeita no final. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AtivoApiIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AtivoRepository ativoRepository;

    private Usuario analista;
    private Usuario auditor;
    private UsuarioLogado analistaLogado;
    private UsuarioLogado auditorLogado;

    @BeforeEach
    void setUp() {
        analista = salvarUsuario("Rafael Api Inv", "rafael.apiinv@empresa.com", Perfil.ANALISTA, true);
        auditor = salvarUsuario("Beatriz Api Inv", "beatriz.apiinv@empresa.com", Perfil.AUDITOR, true);
        salvarUsuario("Tiago Api Inv", "tiago.apiinv@empresa.com", Perfil.ANALISTA, false);
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        auditorLogado = UsuarioLogado.de(auditor, Instant.now());
    }

    private Usuario salvarUsuario(String nome, String email, Perfil perfil, boolean ativo) {
        Usuario u = new Usuario(nome, email, perfil, ativo, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        return usuarioRepository.save(u);
    }

    private Ativo salvarAtivo(String codigo) {
        DadosAtivo dados = new DadosAtivo("Notebooks API", TipoAtivo.HARDWARE, null, null, analista, false, null,
                Classificacao.INTERNA, 3, 2, 3);
        return ativoRepository.save(new Ativo(codigo, dados, analista, Instant.now()));
    }

    private String json(Long proprietarioId) {
        return "{\"nome\":\"CRM API\",\"tipo\":\"INFORMACAO\",\"proprietarioId\":" + proprietarioId
                + ",\"terceirizado\":true,\"fornecedor\":\"Salesforce\",\"classificacao\":\"CONFIDENCIAL\","
                + "\"confidencialidade\":5,\"integridade\":4,\"disponibilidade\":3}";
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void deveListarAtivosParaAuditor() throws Exception {
        // Arrange
        salvarAtivo("ATV-API1");

        // Act
        MvcResult resultado = mvc.perform(get("/api/ativos").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ATV-API1\"", "\"criticidade\":\"MEDIA\"");
    }

    @Test
    void deveNegarCadastroParaAuditor() throws Exception {
        // Arrange — auditor logado

        // Act
        MvcResult resultado = mvc.perform(post("/api/ativos").with(user(auditorLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json(analista.getId()))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ACESSO_NEGADO\"");
    }

    @Test
    void deveCadastrarAtivoPelaApi() throws Exception {
        // Arrange — JSON válido

        // Act
        MvcResult resultado = mvc.perform(post("/api/ativos").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json(analista.getId()))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ATV-", "\"fornecedor\":\"Salesforce\"", "\"criticidade\":\"MUITO_ALTA\"");
    }

    @Test
    void deveResponder422QuandoAuditorForProprietario() throws Exception {
        // Arrange — proprietarioId = auditor

        // Act
        MvcResult resultado = mvc.perform(post("/api/ativos").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json(auditor.getId()))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("\"campo\":\"proprietarioId\"");
    }

    @Test
    void deveResponder400QuandoFaltaremNotasCID() throws Exception {
        // Arrange — sem confidencialidade/integridade/disponibilidade
        String semNotas = "{\"nome\":\"CRM API\",\"tipo\":\"INFORMACAO\",\"proprietarioId\":" + analista.getId()
                + ",\"classificacao\":\"INTERNA\"}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/ativos").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(semNotas)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado)).contains("\"confidencialidade\"", "\"integridade\"", "\"disponibilidade\"");
    }

    @Test
    void deveOferecerSoGerentesEAnalistasAtivosComoProprietarios() throws Exception {
        // Arrange — setUp tem analista ativo, auditor e analista inativo

        // Act
        MvcResult resultado = mvc.perform(get("/api/ativos/proprietarios").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(corpo(resultado)).contains("Rafael Api Inv").doesNotContain("Beatriz Api Inv", "Tiago Api Inv");
    }

    @Test
    void deveAtualizarAtivoPelaApi() throws Exception {
        // Arrange
        Ativo ativo = salvarAtivo("ATV-API2");

        // Act
        MvcResult resultado = mvc.perform(put("/api/ativos/" + ativo.getId()).with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json(analista.getId()))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ATV-API2\"", "\"nome\":\"CRM API\"");
    }

    @Test
    void deveDescontinuarAtivoPelaApi() throws Exception {
        // Arrange
        Ativo ativo = salvarAtivo("ATV-API3");

        // Act
        MvcResult resultado = mvc.perform(post("/api/ativos/" + ativo.getId() + "/descontinuar").with(user(analistaLogado))
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Equipamentos doados\"}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"situacao\":\"DESCONTINUADO\"");
    }
}
