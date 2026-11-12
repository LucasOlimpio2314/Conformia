package io.github.lucasolimpio.conformia.inventario.api;

import io.github.lucasolimpio.conformia.inventario.dto.AtivoForm;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoResumo;
import io.github.lucasolimpio.conformia.inventario.dto.DescontinuarForm;
import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.inventario.service.AtivoService;
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

/**
 * Inventário no app. Leitura para todos os perfis; alterações só Gerente
 * e Analista (SecurityConfig + AtivoService). Mesmo service da tela web.
 */
@RestController
@RequestMapping("/api/ativos")
public class AtivoApiController {

    private final AtivoService ativoService;

    public AtivoApiController(AtivoService ativoService) {
        this.ativoService = ativoService;
    }

    @GetMapping
    public List<AtivoResumo> listar() {
        return ativoService.listar();
    }

    @GetMapping("/{id}")
    public AtivoResumo buscar(@PathVariable Long id) {
        return ativoService.buscar(id);
    }

    /** Opções do campo "proprietário" no formulário do app. */
    @GetMapping("/proprietarios")
    public List<ProprietarioOpcao> proprietarios() {
        return ativoService.proprietariosPossiveis();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtivoResumo criar(@Valid @RequestBody AtivoForm form, @AuthenticationPrincipal UsuarioLogado eu) {
        return ativoService.criar(form, eu.getId());
    }

    @PutMapping("/{id}")
    public AtivoResumo atualizar(@PathVariable Long id, @Valid @RequestBody AtivoForm form,
                                 @AuthenticationPrincipal UsuarioLogado eu) {
        return ativoService.atualizar(id, form, eu.getId());
    }

    /** Body: {"justificativa": "..."} */
    @PostMapping("/{id}/descontinuar")
    public AtivoResumo descontinuar(@PathVariable Long id, @RequestBody DescontinuarForm form,
                                    @AuthenticationPrincipal UsuarioLogado eu) {
        return ativoService.descontinuar(id, form.justificativa(), eu.getId());
    }
}
