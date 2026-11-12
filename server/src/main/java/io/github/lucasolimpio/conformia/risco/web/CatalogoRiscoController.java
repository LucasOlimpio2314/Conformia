package io.github.lucasolimpio.conformia.risco.web;

import io.github.lucasolimpio.conformia.risco.dto.ItemCatalogo;
import io.github.lucasolimpio.conformia.risco.service.CatalogoRiscoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Botão "+ Nova" do drawer de risco (tela web, com CSRF). Responde JSON
 * para o app.js incluir a opção no select sem recarregar o formulário.
 * Por ser @RestController, erros saem em JSON pelo ApiExceptionHandler.
 */
@RestController
@RequestMapping("/riscos/catalogo")
public class CatalogoRiscoController {

    private final CatalogoRiscoService catalogoService;

    public CatalogoRiscoController(CatalogoRiscoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @PostMapping("/ameacas")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCatalogo novaAmeaca(@RequestParam(required = false) String nome, @AuthenticationPrincipal UsuarioLogado eu) {
        return catalogoService.cadastrarAmeaca(nome, eu.getId());
    }

    @PostMapping("/vulnerabilidades")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCatalogo novaVulnerabilidade(@RequestParam(required = false) String nome,
                                            @AuthenticationPrincipal UsuarioLogado eu) {
        return catalogoService.cadastrarVulnerabilidade(nome, eu.getId());
    }
}
