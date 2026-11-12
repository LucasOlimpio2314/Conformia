package io.github.lucasolimpio.conformia.shared.security;

import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Usuário desativado pelo Gerente perde o acesso na próxima requisição,
 * mesmo com a sessão aberta (sem este filtro, só depois de 30 min).
 * Custo: uma consulta por chave primária a cada requisição autenticada.
 * - Web: encerra a sessão e manda para /login?desativado.
 * - API: encerra a sessão e responde 401 USUARIO_INATIVO.
 *
 * Não é @Component (ver TrocaSenhaObrigatoriaFilter).
 */
public class UsuarioAtivoFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;

    public UsuarioAtivoFilter(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioLogado eu
                && !usuarioService.estaAtivo(eu.getId())) {
            SecurityContextHolder.clearContext();
            HttpSession sessao = request.getSession(false);
            if (sessao != null) {
                sessao.invalidate();
            }
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            if (caminho.startsWith("/api/")) {
                RespostaJson.escrever(response, HttpServletResponse.SC_UNAUTHORIZED, "USUARIO_INATIVO",
                        "Seu acesso foi desativado. Fale com um Gerente.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login?desativado");
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
