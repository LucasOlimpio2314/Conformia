package io.github.lucasolimpio.conformia.aviso.web;

import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Avisos do próprio usuário (hoje só Gerentes recebem). Formulários comuns, sem fetch. */
@Controller
@RequestMapping("/avisos")
public class AvisoController {

    private final AvisoService avisoService;

    public AvisoController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @GetMapping
    public String lista(@AuthenticationPrincipal UsuarioLogado eu, Model model) {
        model.addAttribute("avisos", avisoService.listar(eu.getId()));
        return "avisos/lista";
    }

    /** "Ver": marca como lido e leva para a tela do aviso. */
    @PostMapping("/{id}/abrir")
    public String abrir(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu) {
        return "redirect:" + avisoService.abrir(id, eu.getId());
    }

    @PostMapping("/lidos")
    public String marcarTodosLidos(@AuthenticationPrincipal UsuarioLogado eu) {
        avisoService.marcarTodosLidos(eu.getId());
        return "redirect:/avisos";
    }
}
