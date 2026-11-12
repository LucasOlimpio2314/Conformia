package io.github.lucasolimpio.conformia.aceite.service;

import io.github.lucasolimpio.conformia.aceite.domain.AceiteRisco;
import io.github.lucasolimpio.conformia.aceite.domain.SituacaoAceite;
import io.github.lucasolimpio.conformia.aceite.domain.TipoAceite;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteForm;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.dto.PendenciaAceite;
import io.github.lucasolimpio.conformia.aceite.repository.AceiteRiscoRepository;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.RegrasPlano;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.plano.repository.AcaoPlanoRepository;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aceite formal de riscos pelo Gerente (ISO 27001 6.1.3 f).
 * - RISCO: risco ativo com tratamento "Aceitar".
 * - RESIDUAL: risco com plano concluído e residual esperado definido.
 * Vale 12 meses; cai (INVALIDADO) se o risco mudar; o Gerente pode revogar.
 * Nada é apagado: o histórico mostra todos os aceites e por que deixaram de valer.
 */
@Service
@Transactional
public class AceiteService {

    static final int VALIDADE_MESES = 12;
    static final int JUSTIFICATIVA_MINIMO = 20;
    static final int MOTIVO_MINIMO = 10;
    static final int MOTIVO_MAXIMO = 500;
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AceiteRiscoRepository repository;
    private final RiscoRepository riscoRepository;
    private final AcaoPlanoRepository acaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvisoService avisoService;
    private final Clock clock;

    public AceiteService(AceiteRiscoRepository repository, RiscoRepository riscoRepository,
                         AcaoPlanoRepository acaoRepository, UsuarioRepository usuarioRepository,
                         AvisoService avisoService, Clock clock) {
        this.repository = repository;
        this.riscoRepository = riscoRepository;
        this.acaoRepository = acaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.avisoService = avisoService;
        this.clock = clock;
    }

    /**
     * Marca como VENCIDO o que passou dos 12 meses e avisa os Gerentes.
     * Roda todo dia (VencimentoAceiteJob) e também antes de mostrar a tela,
     * para ela nunca mostrar como vigente um aceite já vencido.
     */
    public int atualizarVencidos() {
        Instant agora = clock.instant();
        List<AceiteRisco> vencidos = repository.findBySituacaoAndValidoAteBefore(SituacaoAceite.VIGENTE, agora);
        for (AceiteRisco aceite : vencidos) {
            String data = data(aceite.getValidoAte());
            aceite.vencer("Venceu em " + data + " (validade de " + VALIDADE_MESES + " meses).", agora);
            Risco risco = aceite.getRisco();
            avisoService.notificarGerentes(TipoAviso.ACEITE_VENCIDO, "Aceite do " + risco.getCodigo() + " venceu",
                    "Aceito em " + data(aceite.getAceitoEm()) + ", válido até " + data
                            + ". O risco voltou a aguardar aceite.", "/aceites");
        }
        return vencidos.size();
    }

    /** Riscos aguardando aceite, do mais grave para o mais leve. */
    public List<PendenciaAceite> pendencias() {
        atualizarVencidos();
        Map<Long, List<AcaoPlano>> acoesPorRisco = acoesPorRisco();
        List<PendenciaAceite> pendencias = new ArrayList<>();
        for (Risco risco : riscoRepository.listarCompleto()) {
            if (!risco.isAtivo() || repository.findByRiscoIdAndSituacao(risco.getId(), SituacaoAceite.VIGENTE).isPresent()) {
                continue;
            }
            List<AcaoPlano> acoes = acoesPorRisco.getOrDefault(risco.getId(), List.of());
            tipoPendente(risco, acoes).ifPresent(tipo -> pendencias.add(pendencia(risco, tipo, acoes)));
        }
        pendencias.sort(Comparator.comparingInt(PendenciaAceite::valor).reversed());
        return pendencias;
    }

    @Transactional(readOnly = true)
    public long contarPendentes() {
        Map<Long, List<AcaoPlano>> acoesPorRisco = acoesPorRisco();
        Map<Long, AceiteRisco> vigentes = vigentes();
        Instant agora = clock.instant();
        return riscoRepository.findAll().stream()
                .filter(Risco::isAtivo)
                .filter(r -> !vigentes.containsKey(r.getId()) || vigentes.get(r.getId()).venceuEm(agora))
                .filter(r -> tipoPendente(r, acoesPorRisco.getOrDefault(r.getId(), List.of())).isPresent())
                .count();
    }

    /** Pendência de um risco (drawer de aceite). */
    public PendenciaAceite pendencia(Long riscoId) {
        return pendencias().stream().filter(p -> p.risco().id().equals(riscoId)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Este risco não está aguardando aceite."));
    }

    @Transactional(readOnly = true)
    public List<AceiteResumo> historico() {
        return repository.historico().stream().map(AceiteResumo::de).toList();
    }

    /** Aceites vigentes por id do risco (lista de riscos e plano mostram "aceito até"). */
    @Transactional(readOnly = true)
    public Map<Long, AceiteResumo> vigentesPorRisco() {
        return vigentes().values().stream().map(AceiteResumo::de)
                .collect(Collectors.toMap(AceiteResumo::riscoId, Function.identity()));
    }

    public AceiteResumo aceitar(Long riscoId, AceiteForm form, Long gerenteId) {
        Usuario gerente = exigirGerente(gerenteId);
        PendenciaAceite pendencia = pendencia(riscoId); // também recusa risco que não está aguardando
        String justificativa = form.justificativa().trim();
        if (justificativa.length() < JUSTIFICATIVA_MINIMO) {
            throw new RegraNegocioException("justificativa",
                    "A justificativa precisa ter entre " + JUSTIFICATIVA_MINIMO + " e 1000 caracteres.");
        }
        if (!form.declaracao()) {
            throw new RegraNegocioException("declaracao", "Marque a declaração de aceite.");
        }
        Instant agora = clock.instant();
        Instant validoAte = agora.atZone(clock.getZone()).plusMonths(VALIDADE_MESES).toInstant();
        String condicoes = form.condicoes() == null || form.condicoes().isBlank() ? null : form.condicoes().trim();

        AceiteRisco aceite = repository.save(new AceiteRisco(riscoRepository.getReferenceById(riscoId),
                pendencia.tipo(), pendencia.probabilidade(), pendencia.impacto(), justificativa, condicoes,
                gerente, agora, validoAte));
        return AceiteResumo.de(aceite);
    }

    public AceiteResumo revogar(Long aceiteId, String motivo, Long gerenteId) {
        Usuario gerente = exigirGerente(gerenteId);
        AceiteRisco aceite = repository.findById(aceiteId)
                .orElseThrow(() -> new RegraNegocioException("Aceite não encontrado."));
        if (!aceite.isVigente()) {
            throw new RegraNegocioException("Só um aceite vigente pode ser revogado.");
        }
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.length() < MOTIVO_MINIMO || texto.length() > MOTIVO_MAXIMO) {
            throw new RegraNegocioException("justificativa", "Explique a revogação (entre "
                    + MOTIVO_MINIMO + " e " + MOTIVO_MAXIMO + " caracteres).");
        }
        aceite.revogar(gerente, texto, clock.instant());
        return AceiteResumo.de(aceite);
    }

    /**
     * Chamado quando o risco ou o plano mudam. Se havia aceite vigente,
     * ele cai e os Gerentes recebem um aviso com o que mudou e quem mudou.
     * Se o risco foi encerrado, cai sem aviso (não há mais o que aceitar).
     */
    public void invalidarSeVigente(Risco risco, Usuario quemAlterou, String oQueMudou, boolean avisar) {
        repository.findByRiscoIdAndSituacao(risco.getId(), SituacaoAceite.VIGENTE).ifPresent(aceite -> {
            Instant agora = clock.instant();
            String motivo = oQueMudou + " por " + quemAlterou.getNome() + " em " + data(agora) + ".";
            aceite.invalidar(quemAlterou, motivo, agora);
            if (avisar) {
                avisoService.notificarGerentes(TipoAviso.ACEITE_INVALIDADO,
                        "Aceite do " + risco.getCodigo() + " caiu",
                        motivo + " O risco voltou a aguardar aceite.", "/aceites");
            }
        });
    }

    private Optional<TipoAceite> tipoPendente(Risco risco, List<AcaoPlano> acoes) {
        if (risco.getTratamento() == OpcaoTratamento.ACEITAR) {
            return Optional.of(TipoAceite.RISCO);
        }
        if (RegrasPlano.prontoParaAceite(risco, acoes)) {
            return Optional.of(TipoAceite.RESIDUAL);
        }
        return Optional.empty();
    }

    private PendenciaAceite pendencia(Risco risco, TipoAceite tipo, List<AcaoPlano> acoes) {
        int p = tipo == TipoAceite.RISCO ? risco.getProbabilidade() : risco.getProbabilidadeResidual();
        int i = tipo == TipoAceite.RISCO ? risco.getImpacto() : risco.getImpactoResidual();
        int concluidas = (int) acoes.stream().filter(a -> a.getSituacao() == SituacaoAcao.CONCLUIDA).count();
        AceiteResumo anterior = repository
                .findFirstByRiscoIdAndSituacaoNotOrderByEncerradoEmDesc(risco.getId(), SituacaoAceite.VIGENTE)
                .map(AceiteResumo::de).orElse(null);
        return new PendenciaAceite(RiscoResumo.de(risco), tipo, p, i, p * i, NivelRisco.de(p * i), concluidas, anterior);
    }

    private Map<Long, List<AcaoPlano>> acoesPorRisco() {
        return acaoRepository.findAll().stream().collect(Collectors.groupingBy(a -> a.getRisco().getId()));
    }

    private Map<Long, AceiteRisco> vigentes() {
        return repository.findBySituacao(SituacaoAceite.VIGENTE).stream()
                .collect(Collectors.toMap(a -> a.getRisco().getId(), Function.identity()));
    }

    private Usuario exigirGerente(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        if (usuario.getPerfil() != Perfil.GERENTE) {
            throw new RegraNegocioException("Só um Gerente pode aceitar ou revogar aceites.");
        }
        return usuario;
    }

    private String data(Instant instante) {
        return DATA.format(instante.atZone(clock.getZone()));
    }
}
