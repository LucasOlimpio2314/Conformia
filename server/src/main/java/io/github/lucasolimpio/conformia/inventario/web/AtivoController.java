package io.github.lucasolimpio.conformia.inventario.web;

import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.Criticidade;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoForm;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoResumo;
import io.github.lucasolimpio.conformia.inventario.service.AtivoService;
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tela do inventário. Todos os perfis veem; só Gerente e Analista alteram
 * (SecurityConfig + AtivoService).
 *
 * Drawer e modal usam fetch:
 * - sucesso → cabeçalho X-Redirect; o app.js recarrega a lista já com o aviso;
 * - erro de formulário → fragmento do drawer com status 422 e as mensagens.
 */
@Controller
@RequestMapping("/inventario")
public class AtivoController {

    private static final int STATUS_FORM_INVALIDO = 422;
    private static final Pattern CODIGO = Pattern.compile("[A-Z]{3}-[A-Z0-9]{1,8}");

    private final AtivoService ativoService;

    public AtivoController(AtivoService ativoService) {
        this.ativoService = ativoService;
    }

    /**
     * ?salvo=ATV-009 / ?descontinuado=ATV-009 vêm do X-Redirect: destacam a linha e mostram o aviso.
     * O código é conferido antes de ir para a tela (parâmetro de URL qualquer pessoa pode digitar).
     */
    @GetMapping
    public String lista(@RequestParam(required = false) String salvo,
                        @RequestParam(required = false) String descontinuado, Model model) {
        if (codigoValido(salvo)) {
            model.addAttribute("destaque", salvo);
            model.addAttribute("toast", salvo + " salvo.");
        } else if (codigoValido(descontinuado)) {
            model.addAttribute("toast", descontinuado + " descontinuado.");
        }
        List<AtivoResumo> ativos = ativoService.listar();
        // contagem por tipo só dos ativos em uso, na ordem do enum, sem tipos zerados
        Map<TipoAtivo, Long> porTipo = new LinkedHashMap<>();
        for (TipoAtivo tipo : TipoAtivo.values()) {
            long total = ativos.stream().filter(a -> a.emUso() && a.tipo() == tipo).count();
            if (total > 0) {
                porTipo.put(tipo, total);
            }
        }
        model.addAttribute("ativos", ativos);
        model.addAttribute("emUso", ativos.stream().filter(AtivoResumo::emUso).count());
        model.addAttribute("porTipo", porTipo);
        model.addAttribute("classificacoes", Classificacao.values());
        return "inventario/lista";
    }

    @GetMapping("/novo/form")
    public String formNovo(Model model) {
        model.addAttribute("form", AtivoForm.vazio());
        return formulario(null, model);
    }

    @GetMapping("/{id}/form")
    public String formEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("form", ativoService.formularioDeEdicao(id));
        return formulario(id, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") AtivoForm form, BindingResult erros,
                        @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                AtivoResumo salvo = ativoService.criar(form, eu.getId());
                return redirecionar(response, "/inventario?salvo=" + salvo.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(null, model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") AtivoForm form, BindingResult erros,
                            @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                AtivoResumo salvo = ativoService.atualizar(id, form, eu.getId());
                return redirecionar(response, "/inventario?salvo=" + salvo.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(id, model);
    }

    /** Erro (ex.: justificativa curta) vira 422 em texto pelo WebExceptionHandler; o modal mostra. */
    @PostMapping("/{id}/descontinuar")
    public String descontinuar(@PathVariable Long id, @RequestParam(required = false) String justificativa,
                               @AuthenticationPrincipal UsuarioLogado eu, HttpServletResponse response) {
        AtivoResumo ativo = ativoService.descontinuar(id, justificativa, eu.getId());
        return redirecionar(response, "/inventario?descontinuado=" + ativo.codigo());
    }

    private String formulario(Long id, Model model) {
        model.addAttribute("ativoId", id);
        if (id != null) {
            model.addAttribute("ativo", ativoService.buscar(id));
        }
        model.addAttribute("tipos", TipoAtivo.values());
        model.addAttribute("classificacoes", Classificacao.values());
        model.addAttribute("escala", Arrays.stream(Criticidade.values()).map(Criticidade::getNota).toList());
        model.addAttribute("proprietarios", ativoService.proprietariosPossiveis());
        return "inventario/form :: drawer";
    }

    /** Sucesso via fetch: o app.js lê o X-Redirect e navega. Corpo vazio. */
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
