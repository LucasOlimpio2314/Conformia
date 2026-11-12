package io.github.lucasolimpio.conformia.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela inicial: o inventário de ativos (todos os perfis veem).
 * Quando houver painel de riscos, "/" passa a ser ele.
 * Flash attributes (toast) sobrevivem a este segundo redirect.
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/inventario";
    }
}
