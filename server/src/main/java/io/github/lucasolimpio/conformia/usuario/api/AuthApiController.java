package io.github.lucasolimpio.conformia.usuario.api;

import io.github.lucasolimpio.conformia.shared.security.LoginSucessoHandler;
import io.github.lucasolimpio.conformia.shared.security.TrocaSenhaObrigatoriaFilter;
import io.github.lucasolimpio.conformia.shared.security.UsuarioLogado;
import io.github.lucasolimpio.conformia.usuario.dto.LoginRequest;
import io.github.lucasolimpio.conformia.usuario.dto.SessaoResposta;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResposta;
import io.github.lucasolimpio.conformia.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login do app por sessão, igual à web: depois do login o servidor devolve o
 * cookie JSESSIONID e o app o envia em todas as chamadas seguintes.
 * Logout: POST /api/auth/logout (tratado pelo Spring Security, responde 204).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final LoginSucessoHandler loginSucesso;
    private final UsuarioService usuarioService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthApiController(AuthenticationManager authenticationManager, LoginSucessoHandler loginSucesso,
                             UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.loginSucesso = loginSucesso;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public SessaoResposta login(@Valid @RequestBody LoginRequest login,
                                HttpServletRequest request, HttpServletResponse response) {
        // senha errada / inativo / expirada → AuthenticationException → 401 no ApiExceptionHandler
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(login.email(), login.senha()));

        // novo id de sessão após o login: evita "fixação de sessão"
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextRepository.saveContext(contexto, request, response);

        UsuarioLogado eu = (UsuarioLogado) autenticacao.getPrincipal();
        boolean precisaTrocar = loginSucesso.prepararSessao(request, eu);
        return new SessaoResposta(UsuarioResposta.de(usuarioService.buscar(eu.getId())), precisaTrocar);
    }

    /** Quem está logado. O app chama ao abrir para saber se a sessão ainda vale. */
    @GetMapping("/me")
    public SessaoResposta me(@AuthenticationPrincipal UsuarioLogado eu, HttpSession sessao) {
        boolean precisaTrocar = sessao.getAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO) != null;
        return new SessaoResposta(UsuarioResposta.de(usuarioService.buscar(eu.getId())), precisaTrocar);
    }

    /** Troca obrigatória da senha temporária. Body: {"novaSenha": "...", "confirmacao": "..."} */
    @PostMapping("/trocar-senha")
    public ResponseEntity<Void> trocarSenha(@RequestBody TrocaSenhaForm form,
                                            @AuthenticationPrincipal UsuarioLogado eu, HttpSession sessao) {
        usuarioService.trocarSenhaTemporaria(eu.getId(), form);
        sessao.removeAttribute(TrocaSenhaObrigatoriaFilter.ATRIBUTO_SESSAO);
        return ResponseEntity.noContent().build();
    }
}
