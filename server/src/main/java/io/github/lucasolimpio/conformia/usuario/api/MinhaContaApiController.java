package io.github.lucasolimpio.conformia.usuario.api;

import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResposta;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Minha conta" no app: qualquer perfil. */
@RestController
@RequestMapping("/api/minha-conta")
public class MinhaContaApiController {

    private final UsuarioService usuarioService;

    public MinhaContaApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public UsuarioResposta minhaConta(@AuthenticationPrincipal UsuarioLogado eu) {
        return UsuarioResposta.de(usuarioService.buscar(eu.getId()));
    }

    /** Body: {"senhaAtual": "...", "novaSenha": "...", "confirmacao": "..."} */
    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody TrocaSenhaForm form,
                                             @AuthenticationPrincipal UsuarioLogado eu) {
        usuarioService.alterarPropriaSenha(eu.getId(), form);
        return ResponseEntity.noContent().build();
    }
}
