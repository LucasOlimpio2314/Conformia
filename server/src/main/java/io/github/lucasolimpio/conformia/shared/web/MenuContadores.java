package io.github.lucasolimpio.conformia.shared.web;

import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.plano.service.PlanoAcaoService;
import org.springframework.stereotype.Component;

/**
 * Números do menu lateral. O template chama direto o bean: ${@menu.acoesAtrasadas()}.
 * Assim as consultas só rodam quando o menu é desenhado (não em cada fragmento/fetch).
 */
@Component("menu")
public class MenuContadores {

    private final AvisoService avisoService;
    private final AceiteService aceiteService;
    private final PlanoAcaoService planoService;

    public MenuContadores(AvisoService avisoService, AceiteService aceiteService, PlanoAcaoService planoService) {
        this.avisoService = avisoService;
        this.aceiteService = aceiteService;
        this.planoService = planoService;
    }

    public long avisosNaoLidos(Long usuarioId) {
        return avisoService.contarNaoLidos(usuarioId);
    }

    public long aceitesPendentes() {
        return aceiteService.contarPendentes();
    }

    public long acoesAtrasadas() {
        return planoService.contarAtrasadas();
    }
}
