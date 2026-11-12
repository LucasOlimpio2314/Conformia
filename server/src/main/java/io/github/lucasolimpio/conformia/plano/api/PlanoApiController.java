package io.github.lucasolimpio.conformia.plano.api;

import io.github.lucasolimpio.conformia.plano.dto.AcaoForm;
import io.github.lucasolimpio.conformia.plano.dto.AcaoResumo;
import io.github.lucasolimpio.conformia.plano.dto.IndicadoresPlano;
import io.github.lucasolimpio.conformia.plano.dto.OpcoesAcao;
import io.github.lucasolimpio.conformia.plano.dto.PlanoRisco;
import io.github.lucasolimpio.conformia.plano.dto.ResidualForm;
import io.github.lucasolimpio.conformia.plano.service.PlanoAcaoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Plano de ação no app. Leitura para todos; alterações só Gerente e Analista. */
@RestController
@RequestMapping("/api/plano")
public class PlanoApiController {

    private final PlanoAcaoService planoService;

    public PlanoApiController(PlanoAcaoService planoService) {
        this.planoService = planoService;
    }

    @GetMapping
    public List<PlanoRisco> planos() {
        return planoService.planos();
    }

    @GetMapping("/indicadores")
    public IndicadoresPlano indicadores() {
        return planoService.indicadores(planoService.planos());
    }

    @GetMapping("/{riscoId}")
    public PlanoRisco plano(@PathVariable Long riscoId) {
        return planoService.plano(riscoId);
    }

    /** Responsáveis possíveis e controles do risco, para o formulário da ação. */
    @GetMapping("/{riscoId}/opcoes")
    public OpcoesAcao opcoes(@PathVariable Long riscoId) {
        return planoService.opcoes(riscoId);
    }

    @PostMapping("/{riscoId}/acoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AcaoResumo criarAcao(@PathVariable Long riscoId, @Valid @RequestBody AcaoForm form,
                                @AuthenticationPrincipal UsuarioLogado eu) {
        return planoService.criarAcao(riscoId, form, eu.getId());
    }

    @PutMapping("/acoes/{id}")
    public AcaoResumo atualizarAcao(@PathVariable Long id, @Valid @RequestBody AcaoForm form,
                                    @AuthenticationPrincipal UsuarioLogado eu) {
        return planoService.atualizarAcao(id, form, eu.getId());
    }

    @PutMapping("/{riscoId}/residual")
    public PlanoRisco definirResidual(@PathVariable Long riscoId, @Valid @RequestBody ResidualForm form,
                                      @AuthenticationPrincipal UsuarioLogado eu) {
        return planoService.definirResidual(riscoId, form, eu.getId());
    }
}
