package io.github.lucasolimpio.conformia.inventario.repository;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.SituacaoAtivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AtivoRepository extends JpaRepository<Ativo, Long> {

    /** Próximo número para o código ATV-xxx (sequência do PostgreSQL, criada na V2). */
    @Query(value = "SELECT nextval('ativo_codigo_seq')", nativeQuery = true)
    long proximoNumeroCodigo();

    /**
     * "join fetch" traz o proprietário na mesma consulta. Sem isso, a lista
     * faria 1 consulta para os ativos + 1 por ativo para o proprietário (problema N+1).
     */
    @Query("select a from Ativo a join fetch a.proprietario order by a.id")
    List<Ativo> listarComProprietario();

    /** Ativos que podem ser ligados a um risco novo. */
    List<Ativo> findBySituacaoOrderByCodigoAsc(SituacaoAtivo situacao);
}
