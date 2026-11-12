package io.github.lucasolimpio.conformia.shared.security;

import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Depois do login:
 * - senha temporária → marca a sessão e manda para a troca obrigatória;
 * - senha pessoal    → registra o último acesso e vai para a tela inicial.
 * O login da API (AuthApiController) reaproveita prepararSessao.
 */
@Component
public class LoginSucessoHandler implements AuthenticationSuccessHandler {

    private final UsuarioService usuarioService;

    public LoginSucessoHandler(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        boolean precisaTrocar = prepararSessao(request, (UsuarioLogado) authentication.getPrincipal());
        response.sendRedirect(request.getContextPath() + (precisaTrocar ? TrocaSenhaObrigatoriaFilter.URL_TROCA : "/"));
    }

    /** @return true se a pessoa entrou com senha temporária e precisa trocá-la. */
    public boolean prepararSessao(HttpServletRequest request, UsuarioLogado usuario) {
        if (usuario.isSenhaTemporaria()) {
            request.getSession().setAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO, Boolean.TRUE);
            return true;
        }
        usuarioService.registrarAcesso(usuario.getId());
        return false;
    }
}
