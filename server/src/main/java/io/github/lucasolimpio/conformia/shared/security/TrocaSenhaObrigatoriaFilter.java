package io.github.lucasolimpio.conformia.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Enquanto a pessoa não trocar a senha temporária, só a troca (e o logout)
 * ficam disponíveis. Sem este filtro, bastaria digitar /usuarios na barra de
 * endereço (ou chamar /api/usuarios no app) para pular a troca.
 * - Web: redireciona para /trocar-senha.
 * - API: responde 403 com código TROCA_SENHA_OBRIGATORIA.
 *
 * Não é @Component de propósito: se fosse, o Spring Boot também o registraria
 * como filtro do servidor, fora da cadeia de segurança (rodaria duas vezes).
 */
public class TrocaSenhaObrigatoriaFilter extends OncePerRequestFilter {

    /** Marcado na sessão no login; removido após a troca. */
    public static final String ATRIBUTO_SESSAO = "trocaSenhaObrigatoria";
    public static final String URL_TROCA = "/trocar-senha";

    private static final List<String> LIBERADOS_WEB = List.of(URL_TROCA, "/logout", "/css/", "/js/", "/error");
    private static final List<String> LIBERADOS_API = List.of("/api/auth/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        HttpSession sessao = request.getSession(false);
        boolean precisaTrocar = sessao != null && sessao.getAttribute(ATRIBUTO_SESSAO) != null;
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        boolean api = caminho.startsWith("/api/");
        List<String> liberados = api ? LIBERADOS_API : LIBERADOS_WEB;

        if (!precisaTrocar || liberados.stream().anyMatch(caminho::startsWith)) {
            chain.doFilter(request, response);
        } else if (api) {
            RespostaJson.escrever(response, HttpServletResponse.SC_FORBIDDEN, "TROCA_SENHA_OBRIGATORIA",
                    "Troque a senha temporária antes de continuar.");
        } else {
            response.sendRedirect(request.getContextPath() + URL_TROCA);
        }
    }
}
