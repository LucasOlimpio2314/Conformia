package io.github.lucasolimpio.conformia.plano.web;

import io.github.lucasolimpio.conformia.plano.dto.AcaoForm;
import io.github.lucasolimpio.conformia.plano.dto.AcaoResumo;
import io.github.lucasolimpio.conformia.plano.dto.PlanoRisco;
import io.github.lucasolimpio.conformia.plano.dto.ResidualForm;
import io.github.lucasolimpio.conformia.plano.service.PlanoAcaoService;
import io.github.lucasolimpio.conformia.risco.domain.EscalaRisco;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
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

import java.util.List;
import java.util.TreeSet;

import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.STATUS_FORM_INVALIDO;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.codigoValido;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.redirecionar;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.rejeitar;

/** Tela do plano de ação. Todos leem; Gerente e Analista alteram. */
@Controller
@RequestMapping("/plano")
public class PlanoController {

    private final PlanoAcaoService planoService;

    public PlanoController(PlanoAcaoService planoService) {
        this.planoService = planoService;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String salvo,
                        @RequestParam(required = false) String residual, Model model) {
        if (codigoValido(salvo)) {
            model.addAttribute("destaque", salvo);
            model.addAttribute("toast", salvo + " salva.");
        } else if (codigoValido(residual)) {
            model.addAttribute("toast", "Residual esperado do " + residual + " salvo.");
        }
        List<PlanoRisco> planos = planoService.planos();
        model.addAttribute("planos", planos);
        model.addAttribute("indicadores", planoService.indicadores(planos));
        model.addAttribute("responsaveis", new TreeSet<>(planos.stream()
                .flatMap(p -> p.acoes().stream()).map(AcaoResumo::responsavelNome).toList()));
        return "plano/lista";
    }

    @GetMapping("/{riscoId}/acoes/nova/form")
    public String formNova(@PathVariable Long riscoId, Model model) {
        String primeiroControle = planoService.opcoes(riscoId).controles().getFirst().codigo();
        model.addAttribute("form", AcaoForm.vazio(primeiroControle));
        return formularioAcao(riscoId, null, model);
    }

    @GetMapping("/acoes/{id}/form")
    public String formEdicao(@PathVariable Long id, Model model) {
        AcaoResumo acao = planoService.buscarAcao(id);
        model.addAttribute("form", planoService.formularioDeEdicao(id));
        return formularioAcao(acao.riscoId(), acao, model);
    }

    @PostMapping("/{riscoId}/acoes")
    public String criar(@PathVariable Long riscoId, @Valid @ModelAttribute("form") AcaoForm form, BindingResult erros,
                        @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                AcaoResumo acao = planoService.criarAcao(riscoId, form, eu.getId());
                return redirecionar(response, "/plano?salvo=" + acao.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formularioAcao(riscoId, null, model);
    }

    @PostMapping("/acoes/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") AcaoForm form, BindingResult erros,
                            @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                AcaoResumo acao = planoService.atualizarAcao(id, form, eu.getId());
                return redirecionar(response, "/plano?salvo=" + acao.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        AcaoResumo acao = planoService.buscarAcao(id);
        return formularioAcao(acao.riscoId(), acao, model);
    }

    @GetMapping("/{riscoId}/residual/form")
    public String formResidual(@PathVariable Long riscoId, Model model) {
        PlanoRisco plano = planoService.plano(riscoId);
        model.addAttribute("form", new ResidualForm(plano.probabilidadeResidual(), plano.impactoResidual()));
        return formularioResidual(plano, model);
    }

    @PostMapping("/{riscoId}/residual")
    public String definirResidual(@PathVariable Long riscoId, @Valid @ModelAttribute("form") ResidualForm form,
                                  BindingResult erros, @AuthenticationPrincipal UsuarioLogado eu, Model model,
                                  HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                PlanoRisco plano = planoService.definirResidual(riscoId, form, eu.getId());
                return redirecionar(response, "/plano?residual=" + plano.codigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formularioResidual(planoService.plano(riscoId), model);
    }

    private String formularioAcao(Long riscoId, AcaoResumo acao, Model model) {
        model.addAttribute("plano", planoService.plano(riscoId));
        model.addAttribute("acao", acao);
        model.addAttribute("opcoes", planoService.opcoes(riscoId));
        return "plano/acao-form :: drawer";
    }

    private String formularioResidual(PlanoRisco plano, Model model) {
        model.addAttribute("plano", plano);
        model.addAttribute("probabilidades", EscalaRisco.probabilidades());
        model.addAttribute("impactos", EscalaRisco.impactos());
        model.addAttribute("niveis", NivelRisco.values());
        return "plano/residual-form :: drawer";
    }
}
