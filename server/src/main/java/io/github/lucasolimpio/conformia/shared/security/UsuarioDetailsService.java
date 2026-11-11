package io.github.lucasolimpio.conformia.shared.security;

import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

/**
 * Ponte entre o login do Spring Security e a tabela usuario.
 * O Spring chama loadUserByUsername com o e-mail digitado e depois
 * compara a senha com o hash usando o PasswordEncoder.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;
    private final Clock clock;

    public UsuarioDetailsService(UsuarioRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return repository.findByEmail(normalizado)
                .map(u -> UsuarioLogado.de(u, clock.instant()))
                // a mensagem não chega à tela: o Spring troca por "credenciais inválidas"
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
