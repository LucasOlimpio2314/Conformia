package io.github.lucasolimpio.conformia.usuario.repository;

import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findAllByOrderByNomeAsc();
}
