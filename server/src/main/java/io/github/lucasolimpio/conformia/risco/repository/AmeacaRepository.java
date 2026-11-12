package io.github.lucasolimpio.conformia.risco.repository;

import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmeacaRepository extends JpaRepository<Ameaca, Long> {

    List<Ameaca> findAllByOrderByNomeAsc();

    /** Evita duplicata ignorando maiúsculas ("Phishing" = "phishing"). O banco também garante. */
    boolean existsByNomeIgnoreCase(String nome);
}
