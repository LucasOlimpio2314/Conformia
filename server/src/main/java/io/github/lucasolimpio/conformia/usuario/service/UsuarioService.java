package io.github.lucasolimpio.conformia.usuario.service;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.dto.NovoUsuarioForm;
import io.github.lucasolimpio.conformia.usuario.dto.SenhaTemporariaGerada;
import io.github.lucasolimpio.conformia.usuario.dto.TrocaSenhaForm;
import io.github.lucasolimpio.conformia.usuario.dto.UsuarioResumo;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Regras de usuário e senha. Web e (futuramente) API chamam daqui,
 * por isso quem pede a ação (solicitanteId) é sempre conferido aqui também,
 * não só na configuração de URLs.
 */
@Service
@Transactional
public class UsuarioService {

    static final Duration VALIDADE_SENHA_TEMPORARIA = Duration.ofHours(24);
    static final int SENHA_MINIMO = 8;
    /** BCrypt só considera 72 bytes; acima disso o encoder recusa. */
    static final int SENHA_MAXIMO = 64;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final GeradorSenha geradorSenha;
    private final Clock clock;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                          GeradorSenha geradorSenha, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.geradorSenha = geradorSenha;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumo> listar() {
        return repository.findAllByOrderByNomeAsc().stream().map(UsuarioResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResumo buscar(Long id) {
        return UsuarioResumo.de(carregar(id));
    }

    public SenhaTemporariaGerada criar(NovoUsuarioForm form, Long solicitanteId) {
        exigirGerente(solicitanteId);
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmail(email)) {
            throw new RegraNegocioException("email", "Já existe um usuário com este e-mail.");
        }

        String senha = geradorSenha.gerar();
        Instant agora = clock.instant();
        Usuario usuario = new Usuario(form.nome().trim(), email, form.perfil(), form.ativo(),
                passwordEncoder.encode(senha), agora.plus(VALIDADE_SENHA_TEMPORARIA), agora);
        repository.save(usuario);
        return new SenhaTemporariaGerada(usuario.getId(), usuario.getNome(), senha);
    }

    public SenhaTemporariaGerada redefinirSenha(Long usuarioId, Long solicitanteId) {
        exigirGerente(solicitanteId);
        if (usuarioId.equals(solicitanteId)) {
            throw new RegraNegocioException("Para alterar a sua própria senha, use Minha conta.");
        }

        Usuario usuario = carregar(usuarioId);
        String senha = geradorSenha.gerar();
        usuario.definirSenhaTemporaria(passwordEncoder.encode(senha),
                clock.instant().plus(VALIDADE_SENHA_TEMPORARIA));
        return new SenhaTemporariaGerada(usuario.getId(), usuario.getNome(), senha);
    }

    /** Troca obrigatória depois de entrar com senha temporária. */
    public void trocarSenhaTemporaria(Long usuarioId, TrocaSenhaForm form) {
        Usuario usuario = carregar(usuarioId);
        if (!usuario.isSenhaTemporaria()) {
            throw new RegraNegocioException("Sua senha já foi definida. Para mudar, use Minha conta.");
        }
        validarNovaSenha(usuario, form);
        usuario.definirSenhaPessoal(passwordEncoder.encode(form.novaSenha()));
        usuario.registrarAcesso(clock.instant());
    }

    /** O próprio usuário muda a senha em "Minha conta", confirmando a atual. */
    public void alterarPropriaSenha(Long usuarioId, TrocaSenhaForm form) {
        Usuario usuario = carregar(usuarioId);
        if (form.senhaAtual() == null || !passwordEncoder.matches(form.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("senhaAtual", "Senha atual incorreta.");
        }
        validarNovaSenha(usuario, form);
        usuario.definirSenhaPessoal(passwordEncoder.encode(form.novaSenha()));
    }

    public void registrarAcesso(Long usuarioId) {
        carregar(usuarioId).registrarAcesso(clock.instant());
    }

    private void validarNovaSenha(Usuario usuario, TrocaSenhaForm form) {
        String nova = form.novaSenha() == null ? "" : form.novaSenha();
        if (nova.length() < SENHA_MINIMO || nova.length() > SENHA_MAXIMO) {
            throw new RegraNegocioException("novaSenha",
                    "A nova senha precisa ter entre " + SENHA_MINIMO + " e " + SENHA_MAXIMO + " caracteres.");
        }
        if (nova.chars().noneMatch(Character::isLetter) || nova.chars().noneMatch(Character::isDigit)) {
            throw new RegraNegocioException("novaSenha", "Use letras e números na nova senha.");
        }
        if (!nova.equals(form.confirmacao())) {
            throw new RegraNegocioException("confirmacao", "As senhas não conferem.");
        }
        if (passwordEncoder.matches(nova, usuario.getSenhaHash())) {
            throw new RegraNegocioException("novaSenha", "A nova senha precisa ser diferente da atual.");
        }
    }

    private void exigirGerente(Long solicitanteId) {
        if (!carregar(solicitanteId).isGerente()) {
            throw new RegraNegocioException("Só um Gerente pode gerenciar usuários.");
        }
    }

    private Usuario carregar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
    }
}
