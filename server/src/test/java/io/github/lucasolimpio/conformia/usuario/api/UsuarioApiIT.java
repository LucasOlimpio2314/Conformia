package io.github.lucasolimpio.conformia.usuario.api;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integração da API do app: aplicação inteira + conformia_test.
 * Cada teste roda numa transação desfeita no final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioApiIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository repository;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;
    @Value("${ADMIN_SENHA}")
    private String adminSenha;

    private UsuarioLogado gerenteLogado;
    private UsuarioLogado analistaLogado;

    @BeforeEach
    void setUp() {
        Usuario admin = repository.findByEmail(adminEmail.toLowerCase(Locale.ROOT)).orElseThrow();
        gerenteLogado = UsuarioLogado.de(admin, Instant.now());

        Usuario analista = repository.save(new Usuario("Rafael Nunes", "rafael.api@empresa.com", Perfil.ANALISTA,
                true, "hash-qualquer", null, Instant.now()));
        analista.definirSenhaPessoal("hash-qualquer");
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
    }

    private static String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MvcResult loginComoAdmin() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + adminEmail + "\",\"senha\":\"" + adminSenha + "\"}")).andReturn();
    }

    // ---------- autenticação ----------

    @Test
    void deveFazerLoginEAvisarQueATrocaDeSenhaEObrigatoria() throws Exception {
        // Arrange — usuário inicial com senha temporária

        // Act
        MvcResult resultado = loginComoAdmin();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"trocaSenhaObrigatoria\":true", "\"perfil\":\"GERENTE\"");
        assertThat(resultado.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void deveRecusarLoginComSenhaErradaEmJson() throws Exception {
        // Arrange — senha incorreta

        // Act
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + adminEmail + "\",\"senha\":\"errada-123\"}")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(corpo(resultado)).contains("\"codigo\":\"CREDENCIAIS_INVALIDAS\"");
    }

    @Test
    void deveExigirTrocaDeSenhaAntesDeUsarOutrasRotas() throws Exception {
        // Arrange — sessão criada por um login com senha temporária
        MockHttpSession sessao = (MockHttpSession) loginComoAdmin().getRequest().getSession(false);

        // Act
        MvcResult resultado = mvc.perform(get("/api/usuarios").session(sessao)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(corpo(resultado)).contains("\"codigo\":\"TROCA_SENHA_OBRIGATORIA\"");
    }

    @Test
    void deveLiberarAsRotasDepoisDeTrocarASenhaTemporaria() throws Exception {
        // Arrange — login com senha temporária + troca
        MockHttpSession sessao = (MockHttpSession) loginComoAdmin().getRequest().getSession(false);
        mvc.perform(post("/api/auth/trocar-senha").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"novaSenha\":\"NovaSenha2026\",\"confirmacao\":\"NovaSenha2026\"}"));

        // Act
        MvcResult resultado = mvc.perform(get("/api/usuarios").session(sessao)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void deveResponder401EmJsonSemLogin() throws Exception {
        // Arrange — nenhuma sessão

        // Act
        MvcResult resultado = mvc.perform(get("/api/minha-conta")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(corpo(resultado)).contains("\"codigo\":\"NAO_AUTENTICADO\"");
    }

    // ---------- usuários (só gerente) ----------

    @Test
    void deveNegarListaDeUsuariosParaAnalista() throws Exception {
        // Arrange — analista logado

        // Act
        MvcResult resultado = mvc.perform(get("/api/usuarios").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(corpo(resultado)).contains("\"codigo\":\"ACESSO_NEGADO\"");
    }

    @Test
    void deveListarUsuariosEmJsonSemExporHashDeSenha() throws Exception {
        // Arrange — gerente logado

        // Act
        MvcResult resultado = mvc.perform(get("/api/usuarios").with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("rafael.api@empresa.com").doesNotContain("senha", "$2");
    }

    @Test
    void deveCriarUsuarioERetornarSenhaTemporaria() throws Exception {
        // Arrange
        String json = "{\"nome\":\"Juliana Prado\",\"email\":\"juliana.api@empresa.com\",\"perfil\":\"ANALISTA\",\"ativo\":true}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/usuarios").with(user(gerenteLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado)).contains("\"nomeUsuario\":\"Juliana Prado\"", "\"senha\":\"");
    }

    @Test
    void deveResponder422QuandoEmailJaExistir() throws Exception {
        // Arrange — e-mail do analista do setUp
        String json = "{\"nome\":\"Outro Rafael\",\"email\":\"rafael.api@empresa.com\",\"perfil\":\"AUDITOR\",\"ativo\":true}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/usuarios").with(user(gerenteLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("\"campo\":\"email\"", "Já existe um usuário com este e-mail.");
    }

    @Test
    void deveResponder400ComCamposInvalidos() throws Exception {
        // Arrange — sem nome e com e-mail inválido
        String json = "{\"nome\":\"\",\"email\":\"nao-e-email\",\"perfil\":\"ANALISTA\",\"ativo\":true}";

        // Act
        MvcResult resultado = mvc.perform(post("/api/usuarios").with(user(gerenteLogado))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado)).contains("\"codigo\":\"DADOS_INVALIDOS\"", "\"nome\"", "\"email\"");
    }

    @Test
    void deveImpedirGerenteDeRedefinirAPropriaSenhaPelaApi() throws Exception {
        // Arrange — gerente tentando gerar temporária para si

        // Act
        MvcResult resultado = mvc.perform(post("/api/usuarios/" + gerenteLogado.getId() + "/redefinir-senha")
                .with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(corpo(resultado)).contains("use Minha conta");
    }

    // ---------- minha conta (qualquer perfil) ----------

    @Test
    void deveMostrarMinhaContaParaAnalista() throws Exception {
        // Arrange — analista logado

        // Act
        MvcResult resultado = mvc.perform(get("/api/minha-conta").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).contains("\"email\":\"rafael.api@empresa.com\"", "\"situacao\":\"ATIVO\"");
    }

    @Test
    void deveFazerLogoutSemTokenCsrf() throws Exception {
        // Arrange — POST sem .with(csrf()), como o app faz

        // Act
        MvcResult resultado = mvc.perform(post("/api/auth/logout").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
    }
}
