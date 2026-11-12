package io.github.lucasolimpio.conformia.aviso.api;

import io.github.lucasolimpio.conformia.aviso.dto.AvisoResumo;
import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Avisos do usuário logado (o app pode mostrar um sininho com o contador). */
@RestController
@RequestMapping("/api/avisos")
public class AvisoApiController {

    private final AvisoService avisoService;

    public AvisoApiController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @GetMapping
    public List<AvisoResumo> listar(@AuthenticationPrincipal UsuarioLogado eu) {
        return avisoService.listar(eu.getId());
    }

    @GetMapping("/nao-lidos")
    public Map<String, Long> naoLidos(@AuthenticationPrincipal UsuarioLogado eu) {
        return Map.of("quantidade", avisoService.contarNaoLidos(eu.getId()));
    }

    /** Marca como lido e devolve o link da tela relacionada: {"link": "/aceites"}. */
    @PostMapping("/{id}/lido")
    public Map<String, String> marcarLido(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu) {
        return Map.of("link", avisoService.abrir(id, eu.getId()));
    }

    @PostMapping("/lidos")
    public ResponseEntity<Void> marcarTodosLidos(@AuthenticationPrincipal UsuarioLogado eu) {
        avisoService.marcarTodosLidos(eu.getId());
        return ResponseEntity.noContent().build();
    }
}
