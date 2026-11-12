package io.github.lucasolimpio.conformia.risco.service;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.controle.repository.ControleAnexoARepository;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.SituacaoAtivo;
import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import io.github.lucasolimpio.conformia.risco.domain.DadosRisco;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.domain.Vulnerabilidade;
import io.github.lucasolimpio.conformia.risco.dto.AtivoOpcao;
import io.github.lucasolimpio.conformia.risco.dto.ControleResumo;
import io.github.lucasolimpio.conformia.risco.dto.ItemCatalogo;
import io.github.lucasolimpio.conformia.risco.dto.OpcoesRisco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoForm;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.RiscoRepository;
import io.github.lucasolimpio.conformia.risco.repository.VulnerabilidadeRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras do registro de riscos. Web e API chamam daqui.
 * - Gerente e Analista cadastram, editam e encerram; Auditor só lê.
 * - Pelo menos um ativo (em uso) e pelo menos um controle do Anexo A.
 * - Dono do risco = Gerente ou Analista ativo.
 * - Nada é apagado: risco é encerrado com justificativa e não é mais editado.
 */
@Service
@Transactional
public class RiscoService {

    static final String PREFIXO_CODIGO = "RSK-";
    static final int JUSTIFICATIVA_MINIMO = 10;
    static final int JUSTIFICATIVA_MAXIMO = 500;
    static final List<Perfil> PERFIS_DONO = List.of(Perfil.GERENTE, Perfil.ANALISTA);

    /** Mais grave primeiro; empate pelo código. */
    static final Comparator<RiscoResumo> POR_NIVEL =
            Comparator.comparingInt(RiscoResumo::valor).reversed().thenComparing(RiscoResumo::codigo);

    private final RiscoRepository repository;
    private final AtivoRepository ativoRepository;
    private final AmeacaRepository ameacaRepository;
    private final VulnerabilidadeRepository vulnerabilidadeRepository;
    private final ControleAnexoARepository controleRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public RiscoService(RiscoRepository repository, AtivoRepository ativoRepository, AmeacaRepository ameacaRepository,
                        VulnerabilidadeRepository vulnerabilidadeRepository, ControleAnexoARepository controleRepository,
                        UsuarioRepository usuarioRepository, Clock clock) {
        this.repository = repository;
        this.ativoRepository = ativoRepository;
        this.ameacaRepository = ameacaRepository;
        this.vulnerabilidadeRepository = vulnerabilidadeRepository;
        this.controleRepository = controleRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RiscoResumo> listar() {
        return repository.listarCompleto().stream().map(RiscoResumo::de).sorted(POR_NIVEL).toList();
    }

    /** Quantidade de riscos ATIVOS por nível, na ordem Crítico → Baixo (cards da tela). */
    @Transactional(readOnly = true)
    public Map<NivelRisco, Long> contarAtivosPorNivel(List<RiscoResumo> riscos) {
        Map<NivelRisco, Long> contagem = new EnumMap<>(NivelRisco.class);
        for (NivelRisco nivel : NivelRisco.values()) {
            contagem.put(nivel, riscos.stream().filter(r -> r.ativo() && r.nivel() == nivel).count());
        }
        return contagem;
    }

    @Transactional(readOnly = true)
    public RiscoResumo buscar(Long id) {
        return RiscoResumo.de(carregar(id));
    }

    @Transactional(readOnly = true)
    public RiscoForm formularioDeEdicao(Long id) {
        Risco risco = carregar(id);
        exigirAtivo(risco);
        return RiscoForm.de(risco);
    }

    /**
     * Opções do formulário. Na edição ({@code riscoId} != null) os ativos já ligados
     * aparecem mesmo se descontinuados, para a pessoa poder mantê-los ou removê-los.
     */
    @Transactional(readOnly = true)
    public OpcoesRisco opcoes(Long riscoId) {
        Map<Long, Ativo> ativos = new LinkedHashMap<>();
        ativoRepository.findBySituacaoOrderByCodigoAsc(SituacaoAtivo.EM_USO).forEach(a -> ativos.put(a.getId(), a));
        if (riscoId != null) {
            carregar(riscoId).getAtivos().forEach(a -> ativos.putIfAbsent(a.getId(), a));
        }
        return new OpcoesRisco(
                ativos.values().stream()
                        .map(a -> new AtivoOpcao(a.getId(), a.getCodigo(), a.getNome(), a.getCriticidade().getNota(),
                                a.getProprietario().getId(), a.isEmUso()))
                        .sorted(Comparator.comparing(AtivoOpcao::codigo)).toList(),
                ameacaRepository.findAllByOrderByNomeAsc().stream().map(x -> new ItemCatalogo(x.getId(), x.getNome())).toList(),
                vulnerabilidadeRepository.findAllByOrderByNomeAsc().stream().map(x -> new ItemCatalogo(x.getId(), x.getNome())).toList(),
                controleRepository.findAllByOrderByTemaAscOrdemAsc().stream().map(ControleResumo::de).toList(),
                usuarioRepository.findByAtivoTrueAndPerfilInOrderByNomeAsc(PERFIS_DONO).stream()
                        .map(u -> new ProprietarioOpcao(u.getId(), u.getNome(), u.getPerfil())).toList());
    }

    public RiscoResumo criar(RiscoForm form, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        DadosRisco dados = validar(form, Set.of());
        String codigo = PREFIXO_CODIGO + String.format("%03d", repository.proximoNumeroCodigo());
        return RiscoResumo.de(repository.save(new Risco(codigo, dados, autor, clock.instant())));
    }

    public RiscoResumo atualizar(Long id, RiscoForm form, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        Risco risco = carregar(id);
        exigirAtivo(risco);
        Set<Long> jaLigados = new HashSet<>();
        risco.getAtivos().forEach(a -> jaLigados.add(a.getId()));
        risco.atualizar(validar(form, jaLigados), autor, clock.instant());
        return RiscoResumo.de(risco);
    }

    public RiscoResumo encerrar(Long id, String justificativa, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        Risco risco = carregar(id);
        exigirAtivo(risco);
        String texto = justificativa == null ? "" : justificativa.trim();
        if (texto.length() < JUSTIFICATIVA_MINIMO || texto.length() > JUSTIFICATIVA_MAXIMO) {
            throw new RegraNegocioException("justificativa", "Explique por que o risco foi encerrado (entre "
                    + JUSTIFICATIVA_MINIMO + " e " + JUSTIFICATIVA_MAXIMO + " caracteres).");
        }
        risco.encerrar(texto, autor, clock.instant());
        return RiscoResumo.de(risco);
    }

    /**
     * Regras que dependem do banco. {@code jaLigados}: ativos que o risco já tinha
     * (na edição podem continuar mesmo se foram descontinuados depois).
     */
    private DadosRisco validar(RiscoForm form, Set<Long> jaLigados) {
        List<Ativo> ativos = ativoRepository.findAllById(form.ativoIds());
        if (ativos.size() != new HashSet<>(form.ativoIds()).size()) {
            throw new RegraNegocioException("ativoIds", "Algum ativo escolhido não foi encontrado.");
        }
        for (Ativo ativo : ativos) {
            if (!ativo.isEmUso() && !jaLigados.contains(ativo.getId())) {
                throw new RegraNegocioException("ativoIds",
                        "O ativo " + ativo.getCodigo() + " está descontinuado e não pode ser ligado a um risco.");
            }
        }

        List<ControleAnexoA> controles = controleRepository.findAllById(form.controles());
        if (controles.size() != new HashSet<>(form.controles()).size()) {
            throw new RegraNegocioException("controles", "Algum controle escolhido não existe no Anexo A.");
        }

        Ameaca ameaca = ameacaRepository.findById(form.ameacaId())
                .orElseThrow(() -> new RegraNegocioException("ameacaId", "Ameaça não encontrada."));
        Vulnerabilidade vulnerabilidade = vulnerabilidadeRepository.findById(form.vulnerabilidadeId())
                .orElseThrow(() -> new RegraNegocioException("vulnerabilidadeId", "Vulnerabilidade não encontrada."));

        Usuario dono = usuarioRepository.findById(form.donoId())
                .orElseThrow(() -> new RegraNegocioException("donoId", "Dono do risco não encontrado."));
        if (dono.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("donoId", "Auditor não pode ser dono de risco.");
        }
        if (!dono.isAtivo()) {
            throw new RegraNegocioException("donoId", "Este usuário está inativo. Escolha um Gerente ou Analista ativo.");
        }

        String cenario = form.cenario() == null || form.cenario().isBlank() ? null : form.cenario().trim();
        return new DadosRisco(form.titulo().trim(), cenario, new HashSet<>(ativos), ameaca, vulnerabilidade,
                new HashSet<>(controles), dono, form.probabilidade(), form.impacto(), form.tratamento());
    }

    private Usuario exigirEditor(Long solicitanteId) {
        Usuario usuario = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        if (usuario.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("O perfil Auditor tem acesso somente de leitura.");
        }
        return usuario;
    }

    private static void exigirAtivo(Risco risco) {
        if (!risco.isAtivo()) {
            throw new RegraNegocioException("Risco encerrado não pode ser alterado.");
        }
    }

    private Risco carregar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RegraNegocioException("Risco não encontrado."));
    }
}
