package io.github.lucasolimpio.conformia.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * O UsuarioLogado é uma cópia guardada na sessão no momento do login.
 * Se a pessoa edita o próprio nome/e-mail, a cópia fica velha (menu lateral
 * mostra o nome antigo). Aqui ela é recarregada do banco, sem novo login.
 */
@Component
public class SessaoUsuario {

    private final UsuarioDetailsService usuarioDetailsService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public SessaoUsuario(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    public void recarregar(String email, HttpServletRequest request, HttpServletResponse response) {
        UsuarioLogado atualizado = (UsuarioLogado) usuarioDetailsService.loadUserByUsername(email);
        atualizado.eraseCredentials(); // o hash não fica na sessão
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                atualizado, null, atualizado.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        contextRepository.saveContext(contexto, request, response);
    }
}
