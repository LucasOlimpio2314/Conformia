package io.github.lucasolimpio.conformia.usuario.web;

import io.github.lucasolimpio.conformia.shared.security.TrocaSenhaObrigatoriaFilter;
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

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integração: aplicação inteira + banco conformia_test (já com a migration V1).
 * Cada teste roda numa transação desfeita no final, então o banco não acumula dados.
 *
 * Em testes, injeção em campo é o padrão do JUnit (a classe não é criada por nós).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioWebIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository repository;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;
    @Value("${ADMIN_SENHA}")
    private String adminSenha;

    private Usuario admin;
    private UsuarioLogado gerenteLogado;
    private UsuarioLogado analistaLogado;

    @BeforeEach
    void setUp() {
        admin = repository.findByEmail(adminEmail.toLowerCase(Locale.ROOT)).orElseThrow();
        gerenteLogado = UsuarioLogado.de(admin, Instant.now());

        Usuario analista = repository.save(new Usuario("Rafael Nunes", "rafael.it@empresa.com", Perfil.ANALISTA,
                true, "hash-qualquer", null, Instant.now()));
        analista.definirSenhaPessoal("hash-qualquer");
        analistaLogado = UsuarioLogado.de(analista, Instant.now());
    }

    @Test
    void deveCriarUsuarioInicialPelaMigrationComSenhaTemporaria() {
        // Arrange — feito pela migration V1 com os dados do .env

        // Act
        Usuario inicial = repository.findByEmail(adminEmail.toLowerCase(Locale.ROOT)).orElseThrow();

        // Assert
        assertThat(inicial.getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(inicial.isSenhaTemporaria()).isTrue();
        assertThat(inicial.getSenhaHash()).startsWith("$2");
    }

    @Test
    void deveLevarParaTrocaDeSenhaQuandoEntrarComSenhaTemporaria() throws Exception {
        // Arrange — usuário inicial tem senha temporária

        // Act
        MvcResult resultado = mvc.perform(formLogin("/login")
                .userParameter("email").user(adminEmail)
                .passwordParam("senha").password(adminSenha)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/trocar-senha");
    }

    @Test
    void deveRecusarLoginComSenhaErradaSemDizerSeOEmailExiste() throws Exception {
        // Arrange — senha incorreta

        // Act
        MvcResult resultado = mvc.perform(formLogin("/login")
                .userParameter("email").user(adminEmail)
                .passwordParam("senha").password("senha-errada-123")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/login?erro");
    }

    @Test
    void deveBloquearOutrasTelasEnquantoSenhaForTemporaria() throws Exception {
        // Arrange — sessão marcada pelo LoginSucessoHandler

        // Act
        MvcResult resultado = mvc.perform(get("/usuarios").with(user(gerenteLogado))
                .sessionAttr(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO, true)).andReturn();

        // Assert
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/trocar-senha");
    }

    @Test
    void deveNegarTelaDeUsuariosParaAnalista() throws Exception {
        // Arrange — analista logado

        // Act
        MvcResult resultado = mvc.perform(get("/usuarios").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void deveListarUsuariosParaGerente() throws Exception {
        // Arrange — gerente logado

        // Act
        MvcResult resultado = mvc.perform(get("/usuarios").with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Rafael Nunes", "rafael.it@empresa.com");
    }

    @Test
    void deveCarregarFormularioDeNovoUsuarioNoDrawer() throws Exception {
        // Arrange — gerente logado

        // Act
        MvcResult resultado = mvc.perform(get("/usuarios/novo/form").with(user(gerenteLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Novo usuário", "name=\"perfil\"");
    }

    @Test
    void deveCriarUsuarioEMostrarSenhaTemporaria() throws Exception {
        // Arrange — dados válidos do drawer

        // Act
        MvcResult resultado = mvc.perform(post("/usuarios").with(user(gerenteLogado)).with(csrf())
                .param("nome", "Juliana Prado")
                .param("email", "juliana.it@empresa.com")
                .param("perfil", "ANALISTA")
                .param("ativo", "true")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Senha temporária de", "Juliana Prado");
        assertThat(repository.findByEmail("juliana.it@empresa.com")).isPresent();
    }

    @Test
    void deveDevolverFormularioComErroQuandoEmailJaExistir() throws Exception {
        // Arrange — e-mail do analista criado no setUp

        // Act
        MvcResult resultado = mvc.perform(post("/usuarios").with(user(gerenteLogado)).with(csrf())
                .param("nome", "Outro Rafael")
                .param("email", "rafael.it@empresa.com")
                .param("perfil", "AUDITOR")
                // checkbox desmarcado: o navegador só manda o marcador do Thymeleaf
                .param("_ativo", "on")).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(resultado.getResponse().getContentAsString()).contains("Já existe um usuário com este e-mail.");
    }

    @Test
    void deveMostrarMinhaContaParaQualquerPerfil() throws Exception {
        // Arrange — analista logado

        // Act
        MvcResult resultado = mvc.perform(get("/minha-conta").with(user(analistaLogado))).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Minha conta", "rafael.it@empresa.com");
    }
}
