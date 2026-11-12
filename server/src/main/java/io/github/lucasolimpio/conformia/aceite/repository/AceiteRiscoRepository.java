package io.github.lucasolimpio.conformia.aceite.repository;

import io.github.lucasolimpio.conformia.aceite.domain.AceiteRisco;
import io.github.lucasolimpio.conformia.aceite.domain.SituacaoAceite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AceiteRiscoRepository extends JpaRepository<AceiteRisco, Long> {

    Optional<AceiteRisco> findByRiscoIdAndSituacao(Long riscoId, SituacaoAceite situacao);

    List<AceiteRisco> findBySituacao(SituacaoAceite situacao);

    /** Vigentes que já passaram da validade (verificação diária). */
    List<AceiteRisco> findBySituacaoAndValidoAteBefore(SituacaoAceite situacao, Instant agora);

    /** Histórico completo com risco e Gerente numa consulta só. */
    @EntityGraph(attributePaths = {"risco", "aceitoPor"})
    @Query("select a from AceiteRisco a order by a.aceitoEm desc")
    List<AceiteRisco> historico();

    /** Último aceite que deixou de valer, para explicar no card "aguardando aceite". */
    Optional<AceiteRisco> findFirstByRiscoIdAndSituacaoNotOrderByEncerradoEmDesc(Long riscoId, SituacaoAceite situacao);
}
