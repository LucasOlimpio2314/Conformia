package io.github.lucasolimpio.conformia.usuario.web;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.shared.security.SessaoUsuario;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.dto.EdicaoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.NovoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResumo;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final SessaoUsuario sessaoUsuario;

    public UsuarioController(UsuarioService usuarioService, SessaoUsuario sessaoUsuario) {
        this.usuarioService = usuarioService;
        this.sessaoUsuario = sessaoUsuario;
    }

    /** ?novo= (criado) e ?editado= (salvo) destacam a linha; editado também mostra o aviso. */
    @GetMapping
    public String lista(@RequestParam(required = false) Long novo,
                        @RequestParam(required = false) Long editado, Model model) {
        List<UsuarioResumo> usuarios = usuarioService.listar();
        Map<String, Long> contagem = Arrays.stream(Perfil.values()).collect(Collectors.toMap(Perfil::name,
                p -> usuarios.stream().filter(u -> u.perfil() == p).count()));
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("contagem", contagem);
        model.addAttribute("novoId", novo != null ? novo : editado);
        if (editado != null) {
            usuarios.stream().filter(u -> u.id().equals(editado)).findFirst()
                    .ifPresent(u -> model.addAttribute("toast", u.primeiroNome() + ": alterações salvas."));
        }
        return "usuarios/lista";
    }

    @GetMapping("/{id}/form")
    public String formEdicao(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu, Model model) {
        UsuarioResumo usuario = usuarioService.buscar(id);
        model.addAttribute("form", EdicaoUsuarioForm.de(usuario));
        return formularioEdicao(usuario, eu, model);
    }

    /** Sucesso: X-Redirect (o app.js recarrega a lista). Erro: drawer com 422. */
    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("form") EdicaoUsuarioForm form,
                         BindingResult erros, @AuthenticationPrincipal UsuarioLogado eu, Model model,
                         HttpServletRequest request, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                UsuarioResumo salvo = usuarioService.editar(id, form, eu.getId());
                if (id.equals(eu.getId())) {
                    sessaoUsuario.recarregar(salvo.email(), request, response); // nome novo no menu lateral
                }
                response.setHeader("X-Redirect", "/usuarios?editado=" + id);
                return "fragments/layout :: vazio";
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formularioEdicao(usuarioService.buscar(id), eu, model);
    }

    private String formularioEdicao(UsuarioResumo usuario, UsuarioLogado eu, Model model) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("proprio", usuario.id().equals(eu.getId()));
        return "usuarios/edicao :: drawer";
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
