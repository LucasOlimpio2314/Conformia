package io.github.lucasolimpio.conformia.inventario.web;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.SituacaoAtivo;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Integração da tela do inventário: aplicação inteira + conformia_test, transação desfeita no final. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventarioWebIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AtivoRepository ativoRepository;

    private Usuario analista;
    private Usuario auditor;
    private Usuario inativo;
    private UsuarioLogado analistaLogado;
    private UsuarioLogado auditorLogado;

    @BeforeEach
    void setUp() {
        analista = salvarUsuario("Rafael Inventario", "rafael.inv@empresa.com", Perfil.ANALISTA, true);
        auditor = salvarUsuario("Beatriz Inventario", "beatriz.inv@empresa.com", Perfil.AUDITOR, true);
        inativo = salvarUsuario("Tiago Inventario", "tiago.inv@empresa.com", Perfil.ANALISTA, false);
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
        auditorLogado = UsuarioLogado.de(auditor, Instant.now());
    }

    private Usuario salvarUsuario(String nome, String email, Perfil perfil, boolean ativo) {
        Usuario u = new Usuario(nome, email, perfil, ativo, "hash", null, Instant.now());
        u.definirSenhaPessoal("hash");
        return usuarioRepository.save(u);
    }

    private Ativo salvarAtivo(String codigo, Usuario proprietario) {
        DadosAtivo dados = new DadosAtivo("Servidor de arquivos IT", TipoAtivo.HARDWARE, null, "Sede", proprietario,
                false, null, Classificacao.INTERNA, 3, 3, 2);
        return ativoRepository.save(new Ativo(codigo, dados, analista, Instant.now()));
    }

    /** POST do drawer com os campos que o navegador envia (inclui marcadores de checkbox do Thymeleaf). */
    private MockHttpServletRequestBuilder postAtivo(String url, Long proprietarioId, boolean terceirizado, String fornecedor) {
        MockHttpServletRequestBuilder req = post(url).with(user(analistaLogado)).with(csrf())
                .param("nome", "ERP Financeiro IT")
                .param("tipo", "SOFTWARE")
                .param("proprietarioId", String.valueOf(proprietarioId))
                .param("_terceirizado", "on")
                .param("fornecedor", fornecedor)
                .param("classificacao", "CONFIDENCIAL")
                .param("confidencialidade", "4").param("integridade", "5").param("disponibilidade", "3");
        return terceirizado ? req.param("terceirizado", "true") : req;
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void deveMostrarInventarioParaAuditorSemBotaoDeCadastro() throws Exception {
        // Arrange — auditor logado

        // Act
        MvcResult resultado = mvc.perform(get("/inventario").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("Modo leitura").doesNotContain("+ Novo ativo");
    }

    @Test
    void deveNegarFormularioDeCadastroParaAuditor() throws Exception {
        // Arrange — auditor logado

        // Act
        MvcResult resultado = mvc.perform(get("/inventario/novo/form").with(user(auditorLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveCadastrarAtivoEMandarRecarregarALista() throws Exception {
        // Arrange — formulário válido

        // Act
        MvcResult resultado = mvc.perform(postAtivo("/inventario", analista.getId(), true, "AWS")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).startsWith("/inventario?salvo=ATV-");
        assertThat(ativoRepository.listarComProprietario())
                .anyMatch(a -> a.getNome().equals("ERP Financeiro IT") && "AWS".equals(a.getFornecedor()));
    }

    @Test
    void deveDevolverFormularioComErroQuandoTerceirizadoSemFornecedor() throws Exception {
        // Arrange — chave ligada, fornecedor vazio

        // Act
        MvcResult resultado = mvc.perform(postAtivo("/inventario", analista.getId(), true, "")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Informe o fornecedor do ativo terceirizado.");
    }

    @Test
    void deveRecusarAuditorComoProprietarioNoFormulario() throws Exception {
        // Arrange — proprietário = auditor

        // Act
        MvcResult resultado = mvc.perform(postAtivo("/inventario", auditor.getId(), false, "")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Auditor não pode ser proprietário de ativo.");
    }

    @Test
    void deveAvisarNaListaQuandoProprietarioEstiverInativo() throws Exception {
        // Arrange
        salvarAtivo("ATV-IT1", inativo);

        // Act
        MvcResult resultado = mvc.perform(get("/inventario").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(corpo(resultado)).contains("ATV-IT1", "Proprietário inativo");
    }

    @Test
    void deveAbrirFormularioDeEdicaoComOsDadosDoAtivo() throws Exception {
        // Arrange
        Ativo ativo = salvarAtivo("ATV-IT2", analista);

        // Act
        MvcResult resultado = mvc.perform(get("/inventario/" + ativo.getId() + "/form").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("Editar ATV-IT2", "Servidor de arquivos IT");
    }

    @Test
    void deveDescontinuarAtivoComJustificativa() throws Exception {
        // Arrange
        Ativo ativo = salvarAtivo("ATV-IT3", analista);

        // Act
        MvcResult resultado = mvc.perform(post("/inventario/" + ativo.getId() + "/descontinuar")
                .with(user(analistaLogado)).with(csrf())
                .param("justificativa", "Servidor desligado e descartado")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Redirect")).isEqualTo("/inventario?descontinuado=ATV-IT3");
        assertThat(ativoRepository.findById(ativo.getId()).orElseThrow().getSituacao())
                .isEqualTo(SituacaoAtivo.DESCONTINUADO);
    }

    @Test
    void deveResponder422EmTextoQuandoJustificativaForCurta() throws Exception {
        // Arrange
        Ativo ativo = salvarAtivo("ATV-IT4", analista);

        // Act
        MvcResult resultado = mvc.perform(post("/inventario/" + ativo.getId() + "/descontinuar")
                .with(user(analistaLogado)).with(csrf()).header("X-Requested-With", "fetch")
                .param("justificativa", "doado")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("Explique por que o ativo foi descontinuado");
    }
}
