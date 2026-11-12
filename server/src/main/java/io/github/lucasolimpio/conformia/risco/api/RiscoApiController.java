package io.github.lucasolimpio.conformia.risco.api;

import io.github.lucasolimpio.conformia.risco.dto.EncerramentoForm;
import io.github.lucasolimpio.conformia.risco.dto.ItemCatalogo;
import io.github.lucasolimpio.conformia.risco.dto.NovoItemCatalogoForm;
import io.github.lucasolimpio.conformia.risco.dto.OpcoesRisco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.service.CatalogoRiscoService;
import io.github.lucasolimpio.conformia.risco.service.RiscoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Registro de riscos no app. Leitura para todos; alterações só Gerente e Analista
 * (SecurityConfig + RiscoService). Mesmos services da tela web.
 */
@RestController
@RequestMapping("/api/riscos")
public class RiscoApiController {

    private final RiscoService riscoService;
    private final CatalogoRiscoService catalogoService;

    public RiscoApiController(RiscoService riscoService, CatalogoRiscoService catalogoService) {
        this.riscoService = riscoService;
        this.catalogoService = catalogoService;
    }

    /** Ordenados do maior para o menor nível. */
    @GetMapping
    public List<RiscoResumo> listar() {
        return riscoService.listar();
    }

    @GetMapping("/{id}")
    public RiscoResumo buscar(@PathVariable Long id) {
        return riscoService.buscar(id);
    }

    /** Ativos, catálogos, controles, donos e escalas para montar o formulário. ?riscoId= na edição. */
    @GetMapping("/opcoes")
    public OpcoesRisco opcoes(@RequestParam(required = false) Long riscoId) {
        return riscoService.opcoes(riscoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiscoResumo criar(@Valid @RequestBody RiscoForm form, @AuthenticationPrincipal UsuarioLogado eu) {
        return riscoService.criar(form, eu.getId());
    }

    @PutMapping("/{id}")
    public RiscoResumo atualizar(@PathVariable Long id, @Valid @RequestBody RiscoForm form,
                                 @AuthenticationPrincipal UsuarioLogado eu) {
        return riscoService.atualizar(id, form, eu.getId());
    }

    /** Body: {"justificativa": "..."} */
    @PostMapping("/{id}/encerrar")
    public RiscoResumo encerrar(@PathVariable Long id, @RequestBody EncerramentoForm form,
                                @AuthenticationPrincipal UsuarioLogado eu) {
        return riscoService.encerrar(id, form.justificativa(), eu.getId());
    }

    /** Body: {"nome": "..."} */
    @PostMapping("/catalogo/ameacas")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCatalogo novaAmeaca(@Valid @RequestBody NovoItemCatalogoForm form, @AuthenticationPrincipal UsuarioLogado eu) {
        return catalogoService.cadastrarAmeaca(form.nome(), eu.getId());
    }

    @PostMapping("/catalogo/vulnerabilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCatalogo novaVulnerabilidade(@Valid @RequestBody NovoItemCatalogoForm form,
                                            @AuthenticationPrincipal UsuarioLogado eu) {
        return catalogoService.cadastrarVulnerabilidade(form.nome(), eu.getId());
    }
}
