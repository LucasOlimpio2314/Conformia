package io.github.lucasolimpio.conformia.controle.repository;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControleAnexoARepository extends JpaRepository<ControleAnexoA, String> {

    /** Na ordem da norma: 5.1, 5.2 ... 5.37, 6.1 ... 8.34. */
    List<ControleAnexoA> findAllByOrderByTemaAscOrdemAsc();
}
