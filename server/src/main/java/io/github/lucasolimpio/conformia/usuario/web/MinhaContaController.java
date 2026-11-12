package io.github.lucasolimpio.conformia.usuario.web;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Qualquer perfil vê os próprios dados e troca a própria senha
 * (confirmando a atual, sem senha temporária).
 */
@Controller
@RequestMapping("/minha-conta")
public class MinhaContaController {

    private final UsuarioService usuarioService;

    public MinhaContaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String tela(@AuthenticationPrincipal UsuarioLogado eu, Model model) {
        model.addAttribute("conta", usuarioService.buscar(eu.getId()));
        return "minha-conta";
    }

    @PostMapping("/senha")
    public String alterarSenha(@ModelAttribute TrocaSenhaForm form, @AuthenticationPrincipal UsuarioLogado eu,
                               Model model, RedirectAttributes redirect) {
        try {
            usuarioService.alterarPropriaSenha(eu.getId(), form);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("campoErro", e.getCampo());
            return tela(eu, model);
        }
        redirect.addFlashAttribute("toast", "Senha alterada.");
        return "redirect:/minha-conta";
    }
}
