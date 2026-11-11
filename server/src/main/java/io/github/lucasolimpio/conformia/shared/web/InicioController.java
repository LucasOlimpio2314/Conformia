package io.github.lucasolimpio.conformia.shared.web;

import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela inicial provisória: enquanto os módulos de risco não existem,
 * o Gerente vai para Usuários e os demais para Minha conta.
 * Flash attributes (toast) sobrevivem a este segundo redirect.
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioLogado eu) {
        return eu.getPerfil() == Perfil.GERENTE ? "redirect:/usuarios" : "redirect:/minha-conta";
    }
}
