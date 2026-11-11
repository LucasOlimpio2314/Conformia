package io.github.lucasolimpio.conformia.usuario.web;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.dto.NovoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResumo;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tela de usuários (só GERENTE, ver SecurityConfig).
 * O drawer e o modal usam fetch: as respostas são fragmentos de HTML.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    /** 422: o app.js entende que voltou o formulário com erros. */
    private static final int STATUS_FORM_INVALIDO = 422;

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) Long novo, Model model) {
        List<UsuarioResumo> usuarios = usuarioService.listar();
        Map<String, Long> contagem = Arrays.stream(Perfil.values()).collect(Collectors.toMap(Perfil::name,
                p -> usuarios.stream().filter(u -> u.perfil() == p).count()));
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("contagem", contagem);
        model.addAttribute("novoId", novo);
        return "usuarios/lista";
    }

    @GetMapping("/novo/form")
    public String formNovo(Model model) {
        model.addAttribute("form", NovoUsuarioForm.vazio());
        return formulario(model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") NovoUsuarioForm form, BindingResult erros,
                        @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                model.addAttribute("gerada", usuarioService.criar(form, eu.getId()));
                return "usuarios/senha-temporaria :: conteudo";
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(model);
    }

    @PostMapping("/{id}/redefinir-senha")
    public String redefinirSenha(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu, Model model) {
        model.addAttribute("gerada", usuarioService.redefinirSenha(id, eu.getId()));
        return "usuarios/senha-temporaria :: conteudo";
    }

    private String formulario(Model model) {
        model.addAttribute("perfis", Perfil.values());
        return "usuarios/form :: drawer";
    }

    /** Erro do service vai para o campo certo do formulário (ou para o topo). */
    private static void rejeitar(BindingResult erros, RegraNegocioException e) {
        if (e.getCampo() != null) {
            erros.rejectValue(e.getCampo(), "regra", e.getMessage());
        } else {
            erros.reject("regra", e.getMessage());
        }
    }
}
