package io.github.lucasolimpio.conformia.usuario.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Só exibe a tela. Quem processa o POST /login é o Spring Security.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication autenticacao) {
        boolean logado = autenticacao != null && !(autenticacao instanceof AnonymousAuthenticationToken);
        return logado ? "redirect:/" : "login";
    }
}
