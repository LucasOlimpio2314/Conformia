package io.github.lucasolimpio.conformia.aceite.web;

import io.github.lucasolimpio.conformia.aceite.dto.AceiteForm;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
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

import java.time.Clock;
import java.time.LocalDate;

import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.STATUS_FORM_INVALIDO;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.codigoValido;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.redirecionar;
import static io.github.lucasolimpio.conformia.shared.web.RespostaFetch.rejeitar;

/** Aceites formais. Todos veem; só o Gerente aceita e revoga (SecurityConfig + AceiteService). */
@Controller
@RequestMapping("/aceites")
public class AceiteController {

    private final AceiteService aceiteService;
    private final Clock clock;

    public AceiteController(AceiteService aceiteService, Clock clock) {
        this.aceiteService = aceiteService;
        this.clock = clock;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String aceito,
                        @RequestParam(required = false) String revogado, Model model) {
        if (codigoValido(aceito)) {
            model.addAttribute("toast", "Aceite do " + aceito + " registrado.");
        } else if (codigoValido(revogado)) {
            model.addAttribute("toast", "Aceite do " + revogado + " revogado. O risco voltou a aguardar aceite.");
        }
        model.addAttribute("pendencias", aceiteService.pendencias());
        model.addAttribute("historico", aceiteService.historico());
        return "aceites/lista";
    }

    @GetMapping("/{riscoId}/form")
    public String form(@PathVariable Long riscoId, Model model) {
        model.addAttribute("form", AceiteForm.vazio());
        return formulario(riscoId, model);
    }

    @PostMapping("/{riscoId}")
    public String aceitar(@PathVariable Long riscoId, @Valid @ModelAttribute("form") AceiteForm form, BindingResult erros,
                          @AuthenticationPrincipal UsuarioLogado eu, Model model, HttpServletResponse response) {
        if (!erros.hasErrors()) {
            try {
                AceiteResumo aceite = aceiteService.aceitar(riscoId, form, eu.getId());
                return redirecionar(response, "/aceites?aceito=" + aceite.riscoCodigo());
            } catch (RegraNegocioException e) {
                rejeitar(erros, e);
            }
        }
        response.setStatus(STATUS_FORM_INVALIDO);
        return formulario(riscoId, model);
    }

    /** Modal de justificativa (mesmo do encerrar risco): erro vira 422 em texto. */
    @PostMapping("/revogacoes/{aceiteId}")
    public String revogar(@PathVariable Long aceiteId, @RequestParam(required = false) String justificativa,
                          @AuthenticationPrincipal UsuarioLogado eu, HttpServletResponse response) {
        AceiteResumo aceite = aceiteService.revogar(aceiteId, justificativa, eu.getId());
        return redirecionar(response, "/aceites?revogado=" + aceite.riscoCodigo());
    }

    private String formulario(Long riscoId, Model model) {
        model.addAttribute("pendencia", aceiteService.pendencia(riscoId));
        model.addAttribute("validoAte", LocalDate.now(clock).plusMonths(12));
        return "aceites/form :: drawer";
    }
}
