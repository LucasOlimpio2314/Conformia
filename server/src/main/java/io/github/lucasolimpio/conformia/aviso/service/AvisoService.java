package io.github.lucasolimpio.conformia.aviso.service;

import io.github.lucasolimpio.conformia.aviso.domain.Aviso;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.dto.AvisoResumo;
import io.github.lucasolimpio.conformia.aviso.repository.AvisoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Cria e lê avisos. Quem gera o aviso (aceite, plano) só chama
 * {@link #notificarGerentes}; se um dia houver e-mail, muda só aqui.
 */
@Service
@Transactional
public class AvisoService {

    private final AvisoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public AvisoService(AvisoRepository repository, UsuarioRepository usuarioRepository, Clock clock) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    /** Um aviso para cada Gerente ativo. */
    public void notificarGerentes(TipoAviso tipo, String titulo, String mensagem, String link) {
        Instant agora = clock.instant();
        usuarioRepository.findByAtivoTrueAndPerfilInOrderByNomeAsc(List.of(Perfil.GERENTE))
                .forEach(g -> repository.save(new Aviso(g, tipo, titulo, mensagem, link, agora)));
    }

    @Transactional(readOnly = true)
    public List<AvisoResumo> listar(Long usuarioId) {
        return repository.findTop100ByDestinatarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(AvisoResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidos(Long usuarioId) {
        return repository.countByDestinatarioIdAndLidoEmIsNull(usuarioId);
    }

    /** Marca como lido e devolve o link para onde a pessoa deve ir. */
    public String abrir(Long avisoId, Long usuarioId) {
        Aviso aviso = repository.findByIdAndDestinatarioId(avisoId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Aviso não encontrado."));
        aviso.marcarLido(clock.instant());
        return aviso.getLink();
    }

    public void marcarTodosLidos(Long usuarioId) {
        Instant agora = clock.instant();
        repository.findByDestinatarioIdAndLidoEmIsNull(usuarioId).forEach(a -> a.marcarLido(agora));
    }
}
