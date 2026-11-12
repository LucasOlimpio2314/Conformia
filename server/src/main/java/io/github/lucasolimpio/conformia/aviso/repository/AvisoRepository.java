package io.github.lucasolimpio.conformia.aviso.repository;

import io.github.lucasolimpio.conformia.aviso.domain.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findTop100ByDestinatarioIdOrderByCriadoEmDesc(Long destinatarioId);

    long countByDestinatarioIdAndLidoEmIsNull(Long destinatarioId);

    List<Aviso> findByDestinatarioIdAndLidoEmIsNull(Long destinatarioId);

    /** Só o próprio destinatário pode marcar o aviso como lido. */
    Optional<Aviso> findByIdAndDestinatarioId(Long id, Long destinatarioId);
}
