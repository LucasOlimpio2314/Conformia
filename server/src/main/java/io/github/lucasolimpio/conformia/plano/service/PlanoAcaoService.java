package io.github.lucasolimpio.conformia.plano.service;

import io.github.lucasolimpio.conformia.aceite.domain.TipoAceite;
import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.aceite.service.AceiteService;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;
import io.github.lucasolimpio.conformia.aviso.service.AvisoService;
import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.DadosAcao;
import io.github.lucasolimpio.conformia.plano.domain.RegrasPlano;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.plano.dto.AcaoForm;
import io.github.lucasolimpio.conformia.plano.dto.AcaoResumo;
import io.github.lucasolimpio.conformia.plano.dto.IndicadoresPlano;
import io.github.lucasolimpio.conformia.plano.dto.OpcoesAcao;
import io.github.lucasolimpio.conformia.plano.dto.PlanoRisco;
import io.github.lucasolimpio.conformia.plano.dto.ResidualForm;
import io.github.lucasolimpio.conformia.plano.repository.AcaoPlanoRepository;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.dto.ControleResumo;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Plano de ação dos riscos com tratamento Mitigar, Transferir ou Evitar.
 * - Gerente e Analista criam e atualizam ações; Auditor só lê.
 * - Responsável = Gerente ou Analista ativo; controle = um dos controles do risco.
 * - Concluir exige evidência; ação concluída/cancelada não muda mais.
 * - Residual esperado nunca maior que o inerente.
 * - Plano concluído → Gerentes avisados para aceitar o residual.
 * - Mudança no plano depois do aceite do residual → aceite cai e Gerentes são avisados.
 */
@Service
@Transactional
public class PlanoAcaoService {

    static final String PREFIXO_CODIGO = "ACT-";
    static final List<Perfil> PERFIS_RESPONSAVEL = List.of(Perfil.GERENTE, Perfil.ANALISTA);

    private final AcaoPlanoRepository repository;
    private final RiscoRepository riscoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AceiteService aceiteService;
    private final AvisoService avisoService;
    private final Clock clock;

    public PlanoAcaoService(AcaoPlanoRepository repository, RiscoRepository riscoRepository,
                            UsuarioRepository usuarioRepository, AceiteService aceiteService,
                            AvisoService avisoService, Clock clock) {
        this.repository = repository;
        this.riscoRepository = riscoRepository;
        this.usuarioRepository = usuarioRepository;
        this.aceiteService = aceiteService;
        this.avisoService = avisoService;
        this.clock = clock;
    }

    /** Um plano por risco que precisa de tratamento, do mais grave para o mais leve. */
    @Transactional(readOnly = true)
    public List<PlanoRisco> planos() {
        LocalDate hoje = LocalDate.now(clock);
        Map<Long, List<AcaoPlano>> acoesPorRisco = repository.listarCompleto().stream()
                .collect(Collectors.groupingBy(a -> a.getRisco().getId()));
        Map<Long, AceiteResumo> aceites = aceiteService.vigentesPorRisco();
        return riscoRepository.findAll().stream()
                .filter(RegrasPlano::temPlano)
                .map(r -> plano(r, acoesPorRisco.getOrDefault(r.getId(), List.of()), aceites.get(r.getId()), hoje))
                .sorted(Comparator.comparingInt(PlanoRisco::valor).reversed().thenComparing(PlanoRisco::codigo))
                .toList();
    }

    @Transactional(readOnly = true)
    public IndicadoresPlano indicadores(List<PlanoRisco> planos) {
        List<AcaoResumo> acoes = planos.stream().flatMap(p -> p.acoes().stream()).toList();
        return new IndicadoresPlano(
                acoes.stream().filter(AcaoResumo::editavel).count(),
                acoes.stream().filter(AcaoResumo::atrasada).count(),
                acoes.stream().filter(a -> a.situacao() == SituacaoAcao.CONCLUIDA).count(),
                planos.stream().filter(PlanoRisco::aguardandoAceite).count());
    }

    /** Contador do menu lateral. */
    @Transactional(readOnly = true)
    public long contarAtrasadas() {
        return repository.countBySituacaoInAndPrazoBefore(
                List.of(SituacaoAcao.PENDENTE, SituacaoAcao.EM_ANDAMENTO), LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public PlanoRisco plano(Long riscoId) {
        Risco risco = carregarRiscoComPlano(riscoId);
        return plano(risco, repository.findByRiscoId(riscoId), aceiteService.vigentesPorRisco().get(riscoId),
                LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public AcaoResumo buscarAcao(Long acaoId) {
        return AcaoResumo.de(carregarAcao(acaoId), LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public AcaoForm formularioDeEdicao(Long acaoId) {
        AcaoPlano acao = carregarAcao(acaoId);
        if (acao.getSituacao().isFinalizada()) {
            throw new RegraNegocioException("Ação " + acao.getSituacao().getRotulo().toLowerCase()
                    + " não pode ser alterada.");
        }
        return AcaoForm.de(acao);
    }

    /** Responsáveis possíveis e os controles do próprio risco. */
    @Transactional(readOnly = true)
    public OpcoesAcao opcoes(Long riscoId) {
        Risco risco = carregarRiscoComPlano(riscoId);
        return new OpcoesAcao(
                usuarioRepository.findByAtivoTrueAndPerfilInOrderByNomeAsc(PERFIS_RESPONSAVEL).stream()
                        .map(u -> new ProprietarioOpcao(u.getId(), u.getNome(), u.getPerfil())).toList(),
                controlesDoRisco(risco).stream().map(ControleResumo::de).toList());
    }

    public AcaoResumo criarAcao(Long riscoId, AcaoForm form, Long autorId) {
        Usuario autor = exigirEditor(autorId);
        Risco risco = carregarRiscoComPlano(riscoId);
        boolean prontoAntes = prontoParaAceite(risco);

        // valida ANTES de gerar o código: tentativa recusada não "queima" um número da sequência
        DadosAcao dados = validar(risco, form);
        String codigo = PREFIXO_CODIGO + String.format("%03d", repository.proximoNumeroCodigo());
        AcaoPlano acao = repository.save(new AcaoPlano(codigo, risco, dados, autor, clock.instant()));

        // ação nova reabre o plano: o residual aceito já não corresponde ao plano
        aceiteService.invalidarSeVigente(risco, autor, "Ação " + codigo + " incluída no plano", true);
        avisarSeFicouPronto(risco, prontoAntes);
        return AcaoResumo.de(acao, LocalDate.now(clock));
    }

    public AcaoResumo atualizarAcao(Long acaoId, AcaoForm form, Long autorId) {
        Usuario autor = exigirEditor(autorId);
        AcaoPlano acao = carregarAcao(acaoId);
        if (acao.getSituacao().isFinalizada()) {
            throw new RegraNegocioException("Ação " + acao.getSituacao().getRotulo().toLowerCase()
                    + " não pode ser alterada.");
        }
        Risco risco = acao.getRisco();
        boolean prontoAntes = prontoParaAceite(risco);
        acao.atualizar(validar(risco, form), autor, clock.instant());
        avisarSeFicouPronto(risco, prontoAntes);
        return AcaoResumo.de(acao, LocalDate.now(clock));
    }

    public PlanoRisco definirResidual(Long riscoId, ResidualForm form, Long autorId) {
        Usuario autor = exigirEditor(autorId);
        Risco risco = carregarRiscoComPlano(riscoId);
        int valor = form.probabilidade() * form.impacto();
        if (valor > risco.getValor()) {
            throw new RegraNegocioException("O residual (" + valor + ") não pode ser maior que o risco inerente ("
                    + risco.getValor() + ").");
        }
        boolean prontoAntes = prontoParaAceite(risco);
        boolean mudou = !Integer.valueOf(form.probabilidade()).equals(risco.getProbabilidadeResidual())
                || !Integer.valueOf(form.impacto()).equals(risco.getImpactoResidual());
        String antes = risco.temResidual()
                ? risco.getProbabilidadeResidual() + "×" + risco.getImpactoResidual() : "não definido";

        risco.definirResidual(form.probabilidade(), form.impacto(), autor, clock.instant());
        if (mudou) {
            aceiteService.invalidarSeVigente(risco, autor, "Residual esperado alterado (" + antes + " → "
                    + form.probabilidade() + "×" + form.impacto() + ")", true);
        }
        avisarSeFicouPronto(risco, prontoAntes);
        return plano(riscoId);
    }

    /** Regras que dependem do banco ou de mais de um campo (formato já veio validado). */
    private DadosAcao validar(Risco risco, AcaoForm form) {
        Usuario responsavel = usuarioRepository.findById(form.responsavelId())
                .orElseThrow(() -> new RegraNegocioException("responsavelId", "Responsável não encontrado."));
        if (responsavel.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("responsavelId", "Auditor não pode ser responsável por ação.");
        }
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException("responsavelId",
                    "Este usuário está inativo. Escolha um Gerente ou Analista ativo.");
        }
        ControleAnexoA controle = controlesDoRisco(risco).stream()
                .filter(c -> c.getCodigo().equals(form.controle())).findFirst()
                .orElseThrow(() -> new RegraNegocioException("controle",
                        "Escolha um dos controles do Anexo A ligados ao risco."));
        String evidencia = form.evidencia() == null || form.evidencia().isBlank() ? null : form.evidencia().trim();
        if (form.situacao() == SituacaoAcao.CONCLUIDA && evidencia == null) {
            throw new RegraNegocioException("evidencia",
                    "Informe a evidência para concluir a ação (nº do chamado, link ou documento).");
        }
        return new DadosAcao(form.titulo().trim(), responsavel, form.prazo(), controle, form.situacao(), evidencia);
    }

    private void avisarSeFicouPronto(Risco risco, boolean prontoAntes) {
        if (!prontoAntes && prontoParaAceite(risco)) {
            avisoService.notificarGerentes(TipoAviso.RESIDUAL_PRONTO,
                    risco.getCodigo() + " pronto para aceite do risco residual",
                    "Todas as ações do plano foram concluídas. Residual esperado: "
                            + risco.getProbabilidadeResidual() + " × " + risco.getImpactoResidual() + " = "
                            + risco.getValorResidual() + " (" + NivelRisco.de(risco.getValorResidual()).getRotulo() + ").",
                    "/aceites");
        }
    }

    private boolean prontoParaAceite(Risco risco) {
        return RegrasPlano.prontoParaAceite(risco, repository.findByRiscoId(risco.getId()));
    }

    private PlanoRisco plano(Risco r, List<AcaoPlano> acoes, AceiteResumo aceite, LocalDate hoje) {
        List<AcaoResumo> resumos = acoes.stream().map(a -> AcaoResumo.de(a, hoje))
                .sorted(Comparator.comparing(AcaoResumo::prazo).thenComparing(AcaoResumo::codigo)).toList();
        int validas = (int) acoes.stream().filter(a -> a.getSituacao() != SituacaoAcao.CANCELADA).count();
        int concluidas = (int) acoes.stream().filter(a -> a.getSituacao() == SituacaoAcao.CONCLUIDA).count();
        AceiteResumo aceiteResidual = aceite != null && aceite.tipo() == TipoAceite.RESIDUAL ? aceite : null;
        Integer valorRes = r.getValorResidual();
        return new PlanoRisco(r.getId(), r.getCodigo(), r.getTitulo(), r.getTratamento(),
                r.getProbabilidade(), r.getImpacto(), r.getValor(), r.getNivel(),
                r.getProbabilidadeResidual(), r.getImpactoResidual(), valorRes,
                valorRes == null ? null : NivelRisco.de(valorRes),
                resumos, concluidas, validas, RegrasPlano.prontoParaAceite(r, acoes), aceiteResidual);
    }

    private static List<ControleAnexoA> controlesDoRisco(Risco risco) {
        return risco.getControles().stream()
                .sorted(Comparator.comparingInt(ControleAnexoA::getTema).thenComparingInt(ControleAnexoA::getOrdem))
                .toList();
    }

    private Risco carregarRiscoComPlano(Long riscoId) {
        Risco risco = riscoRepository.findById(riscoId)
                .orElseThrow(() -> new RegraNegocioException("Risco não encontrado."));
        if (!RegrasPlano.temPlano(risco)) {
            throw new RegraNegocioException("Só riscos ativos com tratamento Mitigar, Transferir ou Evitar têm plano de ação.");
        }
        return risco;
    }

    private AcaoPlano carregarAcao(Long id) {
        return repository.findById(id).orElseThrow(() -> new RegraNegocioException("Ação não encontrada."));
    }

    private Usuario exigirEditor(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        if (usuario.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("O perfil Auditor tem acesso somente de leitura.");
        }
        return usuario;
    }
}
