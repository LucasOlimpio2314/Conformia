package io.github.lucasolimpio.conformia.risco.repository;

import io.github.lucasolimpio.conformia.risco.domain.Vulnerabilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VulnerabilidadeRepository extends JpaRepository<Vulnerabilidade, Long> {

    List<Vulnerabilidade> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);
}
