package io.github.lucasolimpio.conformia.usuario.api;

import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.dto.NovoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.SenhaTemporariaGerada;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResposta;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
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

/**
 * Gestão de usuários no app (só GERENTE, ver SecurityConfig).
 * Mesmo UsuarioService da tela web: nenhuma regra duplicada.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private final UsuarioService usuarioService;

    public UsuarioApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResposta> listar() {
        return usuarioService.listar().stream().map(UsuarioResposta::de).toList();
    }

    /** Body: {"nome", "email", "perfil": "ANALISTA", "ativo": true}. Resposta traz a senha temporária (única vez). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SenhaTemporariaGerada criar(@Valid @RequestBody NovoUsuarioForm form,
                                       @AuthenticationPrincipal UsuarioLogado eu) {
        return usuarioService.criar(form, eu.getId());
    }

    @PostMapping("/{id}/redefinir-senha")
    public SenhaTemporariaGerada redefinirSenha(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado eu) {
        return usuarioService.redefinirSenha(id, eu.getId());
    }
}
