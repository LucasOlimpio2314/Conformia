package io.github.lucasolimpio.conformia.usuario.web;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.shared.security.TrocaSenhaObrigatoriaFilter;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Troca obrigatória de senha depois de entrar com a senha temporária. */
@Controller
@RequestMapping(TrocaSenhaObrigatoriaFilter.URL_TROCA)
public class TrocaSenhaController {

    private final UsuarioService usuarioService;

    public TrocaSenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String tela(@AuthenticationPrincipal UsuarioLogado eu, HttpSession sessao, Model model) {
        if (sessao.getAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO) == null) {
            return "redirect:/"; // não há troca pendente
        }
        model.addAttribute("primeiroNome", eu.getNome().split("\\s+")[0]);
        return "trocar-senha";
    }

    @PostMapping
    public String trocar(@ModelAttribute TrocaSenhaForm form, @AuthenticationPrincipal UsuarioLogado eu,
                         HttpSession sessao, Model model, RedirectAttributes redirect) {
        try {
            usuarioService.trocarSenhaTemporaria(eu.getId(), form);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("campoErro", e.getCampo());
            model.addAttribute("primeiroNome", eu.getNome().split("\\s+")[0]);
            return "trocar-senha";
        }
        sessao.removeAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO);
        redirect.addFlashAttribute("toast", "Senha alterada. Tudo pronto, " + eu.getNome().split("\\s+")[0] + ".");
        return "redirect:/";
    }
}
