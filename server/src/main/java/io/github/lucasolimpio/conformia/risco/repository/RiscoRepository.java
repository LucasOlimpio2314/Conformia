package io.github.lucasolimpio.conformia.risco.repository;

import io.github.lucasolimpio.conformia.risco.domain.Risco;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RiscoRepository extends JpaRepository<Risco, Long> {

    /** Próximo número para o código RSK-xxx (sequência criada na V3). */
    @Query(value = "SELECT nextval('risco_codigo_seq')", nativeQuery = true)
    long proximoNumeroCodigo();

    /**
     * Lista com tudo que a tela mostra numa consulta só (@EntityGraph = "join fetch"
     * declarativo). Sem isso seriam várias consultas por risco (problema N+1).
     */
    @EntityGraph(attributePaths = {"ativos", "controles", "ameaca", "vulnerabilidade", "dono", "atualizadoPor"})
    @Query("select r from Risco r")
    List<Risco> listarCompleto();
}
