package io.github.lucasolimpio.conformia.plano.repository;

import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AcaoPlanoRepository extends JpaRepository<AcaoPlano, Long> {

    @Query(value = "SELECT nextval('acao_codigo_seq')", nativeQuery = true)
    long proximoNumeroCodigo();

    List<AcaoPlano> findByRiscoId(Long riscoId);

    /** Todas as ações com responsável e controle numa consulta só (tela do plano). */
    @EntityGraph(attributePaths = {"risco", "responsavel", "controle"})
    @Query("select a from AcaoPlano a order by a.prazo, a.id")
    List<AcaoPlano> listarCompleto();

    /** Contador do menu: ações em aberto com prazo vencido. */
    long countBySituacaoInAndPrazoBefore(Collection<SituacaoAcao> situacoes, LocalDate hoje);
}
