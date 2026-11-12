package io.github.lucasolimpio.conformia.risco.web;

import io.github.lucasolimpio.conformia.risco.domain.EscalaRisco;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.dto.ControleResumo;
import io.github.lucasolimpio.conformia.risco.dto.OpcoesRisco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.service.RiscoService;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Registro de riscos. Todos os perfis veem; Gerente e Analista alteram.
 * Mesmo padrão do inventário: drawer/modal via fetch, sucesso com X-Redirect, erro com 422.
 */
@Controller
@RequestMapping("/riscos")
public class RiscoController {

    private static final int STATUS_FORM_INVALIDO = 422;
    private static final Pattern CODIGO = Pattern.compile("[A-Z]{3}-[A-Z0-9]{1,8}");

    private final RiscoService riscoService;

    public RiscoController(RiscoService riscoService) {
        this.riscoService = riscoService;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String salvo,
                        @RequestParam(required = false) String encerrado, Model model) {
        if (codigoValido(salvo)) {
            model.addAttribute("destaque", salvo);
            model.addAttribute("toast", salvo + " salvo.");
        } else if (codigoValido(encerrado)) {
            model.addAttribute("toast", encerrado + " encerrado.");
        }
        List<RiscoResumo> riscos = riscoService.listar();
        model.addAttribute("riscos", riscos);
        model.addAttribute("porNivel", riscoService.contarAtivosPorNivel(riscos));
        model.addAttribute("niveis", NivelRisco.values());
        return "riscos/lista";
    }

    @GetMapping("/novo/form")
    public String formNovo(Model model) {
        model.addAttribute("form", RiscoForm.vazio());
        return formulario(null, model);
    }

    @GetMapping("/{id}/form")
    public String formEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("form", riscoService.formularioDeEdicao(id));
        return formulario(id, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") RiscoForm form, BindingResult erros,
                        @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                RiscoResumo salvo = riscoService.criar(form, eu.getId());
                return redirecionar(response, "/riscos?salvo=" + salvo.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(null, model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") RiscoForm form, BindingResult erros,
                            @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                RiscoResumo salvo = riscoService.atualizar(id, form, eu.getId());
                return redirecionar(response, "/riscos?salvo=" + salvo.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(id, model);
    }

    /** Erro (justificativa curta etc.) vira 422 em texto pelo WebExceptionHandler; o modal mostra. */
    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable Long id, @RequestParam(required = false) String justificativa,
                           @AuthenticationPrincipal UsuarioLogado eu, HttpServletResponse response) {
        RiscoResumo risco = riscoService.encerrar(id, justificativa, eu.getId());
        return redirecionar(response, "/riscos?encerrado=" + risco.codigo());
    }

    private String formulario(Long id, Model model) {
        OpcoesRisco opcoes = riscoService.opcoes(id);
        model.addAttribute("riscoId", id);
        if (id != null) {
            model.addAttribute("risco", riscoService.buscar(id));
        }
        model.addAttribute("opcoes", opcoes);
        // controles agrupados por tema (5, 6, 7, 8) para o seletor
        Map<Integer, List<ControleResumo>> porTema = opcoes.controles().stream()
                .collect(Collectors.groupingBy(ControleResumo::tema, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("controlesPorTema", porTema);
        model.addAttribute("probabilidades", EscalaRisco.probabilidades());
        model.addAttribute("impactos", EscalaRisco.impactos());
        model.addAttribute("tratamentos", OpcaoTratamento.values());
        model.addAttribute("niveis", NivelRisco.values());
        return "riscos/form :: drawer";
    }

    private static String redirecionar(HttpServletResponse response, String url) {
        response.setHeader("X-Redirect", url);
        return "fragments/layout :: vazio";
    }

    private static boolean codigoValido(String codigo) {
        return codigo != null && CODIGO.matcher(codigo).matches();
    }

    private static void rejeitar(BindingResult erros, RegraNegocioException e) {
        if (e.getCampo() != null) {
            erros.rejectValue(e.getCampo(), "regra", e.getMessage());
        } else {
            erros.reject("regra", e.getMessage());
        }
    }
}
