package io.github.lucasolimpio.conformia.usuario.repository;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** E-mail em uso por OUTRO usuário (na edição, o próprio e-mail não conta). */
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Conferido a cada requisição: desativado perde a sessão na hora. */
    boolean existsByIdAndAtivoTrue(Long id);

    List<Usuario> findAllByOrderByNomeAsc();

    /** Ex.: usuários ativos com perfil GERENTE ou ANALISTA (possíveis proprietários de ativo). */
    List<Usuario> findByAtivoTrueAndPerfilInOrderByNomeAsc(Collection<Perfil> perfis);
}
