package io.github.lucasolimpio.conformia.aceite.api;

import io.github.lucasolimpio.conformia.aceite.dto.AceiteForm;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.dto.PendenciaAceite;
import io.github.lucasolimpio.conformia.aceite.dto.RevogacaoForm;
import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Aceites no app. Leitura para todos; aceitar e revogar só Gerente. */
@RestController
@RequestMapping("/api/aceites")
public class AceiteApiController {

    private final AceiteService aceiteService;

    public AceiteApiController(AceiteService aceiteService) {
        this.aceiteService = aceiteService;
    }

    @GetMapping
    public List<AceiteResumo> historico() {
        return aceiteService.historico();
    }

    @GetMapping("/pendencias")
    public List<PendenciaAceite> pendencias() {
        return aceiteService.pendencias();
    }

    /** Body: {"justificativa", "condicoes", "declaracao": true} */
    @PostMapping("/riscos/{riscoId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AceiteResumo aceitar(@PathVariable Long riscoId, @Valid @RequestBody AceiteForm form,
                                @AuthenticationPrincipal UsuarioLogado eu) {
        return aceiteService.aceitar(riscoId, form, eu.getId());
    }

    /** Body: {"motivo": "..."} */
    @PostMapping("/{id}/revogar")
    public AceiteResumo revogar(@PathVariable Long id, @RequestBody RevogacaoForm form,
                                @AuthenticationPrincipal UsuarioLogado eu) {
        return aceiteService.revogar(id, form.motivo(), eu.getId());
    }
}
