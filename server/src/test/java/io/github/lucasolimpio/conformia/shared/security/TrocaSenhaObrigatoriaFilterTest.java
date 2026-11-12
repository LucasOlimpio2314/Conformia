package io.github.lucasolimpio.conformia.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unitário: request/response falsos do spring-test, sem subir o Spring.
 * MockFilterChain guarda a requisição se o filtro deixou passar.
 */
class TrocaSenhaObrigatoriaFilterTest {

    private TrocaSenhaObrigatoriaFilter filtro;
    private MockHttpSession sessaoComTrocaPendente;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filtro = new TrocaSenhaObrigatoriaFilter();
        sessaoComTrocaPendente = new MockHttpSession();
        sessaoComTrocaPendente.setAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO, true);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private MockHttpServletRequest requisicao(String uri, MockHttpSession sessao) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setSession(sessao);
        return request;
    }

    @Test
    void deveDeixarPassarQuandoNaoHaTrocaPendente() throws Exception {
        // Arrange
        MockHttpServletRequest request = requisicao("/usuarios", new MockHttpSession());

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void deveRedirecionarTelaWebParaTrocaDeSenha() throws Exception {
        // Arrange
        MockHttpServletRequest request = requisicao("/usuarios", sessaoComTrocaPendente);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertThat(response.getRedirectedUrl()).isEqualTo("/trocar-senha");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void deveLiberarAPropriaTelaDeTrocaDeSenha() throws Exception {
        // Arrange
        MockHttpServletRequest request = requisicao("/trocar-senha", sessaoComTrocaPendente);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void deveResponder403EmJsonNaApi() throws Exception {
        // Arrange
        MockHttpServletRequest request = requisicao("/api/usuarios", sessaoComTrocaPendente);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"codigo\":\"TROCA_SENHA_OBRIGATORIA\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void deveLiberarRotasDeAutenticacaoDaApi() throws Exception {
        // Arrange — o app precisa chamar /api/auth/trocar-senha
        MockHttpServletRequest request = requisicao("/api/auth/trocar-senha", sessaoComTrocaPendente);

        // Act
        filtro.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
