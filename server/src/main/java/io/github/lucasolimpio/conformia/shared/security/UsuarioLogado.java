package io.github.lucasolimpio.conformia.shared.security;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Quem está logado. Fica guardado na sessão, por isso é uma cópia enxuta
 * do Usuario (a entidade JPA não deve ir para a sessão).
 * Depois do login o Spring chama eraseCredentials() e o hash sai da memória.
 */
public class UsuarioLogado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nome;
    private final String email;
    private final Perfil perfil;
    private final boolean ativo;
    private final boolean senhaTemporaria;
    private final boolean senhaExpirada;
    private String senhaHash;

    public UsuarioLogado(Long id, String nome, String email, Perfil perfil, boolean ativo,
                         boolean senhaTemporaria, boolean senhaExpirada, String senhaHash) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.ativo = ativo;
        this.senhaTemporaria = senhaTemporaria;
        this.senhaExpirada = senhaExpirada;
        this.senhaHash = senhaHash;
    }

    public static UsuarioLogado de(Usuario u, Instant agora) {
        return new UsuarioLogado(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                u.isSenhaTemporaria(), u.isSenhaExpirada(agora), u.getSenhaHash());
    }

    /** Spring Security exige o prefixo ROLE_ para hasRole("GERENTE"). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Usuário inativo: o Spring recusa o login (DisabledException). */
    @Override
    public boolean isEnabled() {
        return ativo;
    }

    /** Senha temporária vencida: o Spring recusa o login (CredentialsExpiredException). */
    @Override
    public boolean isCredentialsNonExpired() {
        return !senhaExpirada;
    }

    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isSenhaTemporaria() {
        return senhaTemporaria;
    }

    public String getIniciais() {
        String[] partes = nome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }
}
