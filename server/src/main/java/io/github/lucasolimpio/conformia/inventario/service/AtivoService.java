package io.github.lucasolimpio.conformia.inventario.service;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.DadosAtivo;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoForm;
import io.github.lucasolimpio.conformia.inventario.dto.AtivoResumo;
import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.inventario.repository.AtivoRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Regras do inventário de ativos. Web e API chamam daqui.
 * - Gerente e Analista cadastram e editam; Auditor só lê.
 * - Proprietário é um Gerente ou Analista ativo (Auditor nunca).
 * - Terceirizado exige fornecedor; o proprietário continua interno.
 * - Nada é apagado: ativo é descontinuado com justificativa.
 */
@Service
@Transactional
public class AtivoService {

    static final String PREFIXO_CODIGO = "ATV-";
    static final int JUSTIFICATIVA_MINIMO = 10;
    static final int JUSTIFICATIVA_MAXIMO = 500;
    static final List<Perfil> PERFIS_PROPRIETARIO = List.of(Perfil.GERENTE, Perfil.ANALISTA);

    private final AtivoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public AtivoService(AtivoRepository repository, UsuarioRepository usuarioRepository, Clock clock) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AtivoResumo> listar() {
        return repository.listarComProprietario().stream().map(AtivoResumo::de).toList();
    }

    @Transactional(readOnly = true)
    public AtivoResumo buscar(Long id) {
        return AtivoResumo.de(carregar(id));
    }

    /** Dados para o formulário de edição. Ativo descontinuado não é editável. */
    @Transactional(readOnly = true)
    public AtivoForm formularioDeEdicao(Long id) {
        Ativo ativo = carregar(id);
        exigirEmUso(ativo);
        return AtivoForm.de(ativo);
    }

    @Transactional(readOnly = true)
    public List<ProprietarioOpcao> proprietariosPossiveis() {
        return usuarioRepository.findByAtivoTrueAndPerfilInOrderByNomeAsc(PERFIS_PROPRIETARIO).stream()
                .map(u -> new ProprietarioOpcao(u.getId(), u.getNome(), u.getPerfil()))
                .toList();
    }

    public AtivoResumo criar(AtivoForm form, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        DadosAtivo dados = validar(form);
        String codigo = PREFIXO_CODIGO + String.format("%03d", repository.proximoNumeroCodigo());

        Ativo ativo = repository.save(new Ativo(codigo, dados, autor, clock.instant()));
        return AtivoResumo.de(ativo);
    }

    public AtivoResumo atualizar(Long id, AtivoForm form, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        Ativo ativo = carregar(id);
        exigirEmUso(ativo);
        ativo.atualizar(validar(form), autor, clock.instant());
        return AtivoResumo.de(ativo);
    }

    public AtivoResumo descontinuar(Long id, String justificativa, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        Ativo ativo = carregar(id);
        exigirEmUso(ativo);

        String texto = limpar(justificativa);
        if (texto == null || texto.length() < JUSTIFICATIVA_MINIMO || texto.length() > JUSTIFICATIVA_MAXIMO) {
            throw new RegraNegocioException("justificativa", "Explique por que o ativo foi descontinuado (entre "
                    + JUSTIFICATIVA_MINIMO + " e " + JUSTIFICATIVA_MAXIMO + " caracteres).");
        }
        ativo.descontinuar(texto, autor, clock.instant());
        return AtivoResumo.de(ativo);
    }

    /** Regras que dependem do banco ou de mais de um campo. O formato já veio validado (@Valid). */
    private DadosAtivo validar(AtivoForm form) {
        Usuario proprietario = usuarioRepository.findById(form.proprietarioId())
                .orElseThrow(() -> new RegraNegocioException("proprietarioId", "Proprietário não encontrado."));
        if (proprietario.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("proprietarioId", "Auditor não pode ser proprietário de ativo.");
        }
        if (!proprietario.isAtivo()) {
            throw new RegraNegocioException("proprietarioId",
                    "Este usuário está inativo. Escolha um Gerente ou Analista ativo.");
        }

        String fornecedor = limpar(form.fornecedor());
        if (form.terceirizado() && fornecedor == null) {
            throw new RegraNegocioException("fornecedor", "Informe o fornecedor do ativo terceirizado.");
        }

        return new DadosAtivo(form.nome().trim(), form.tipo(), limpar(form.descricao()), limpar(form.localizacao()),
                proprietario, form.terceirizado(), fornecedor, form.classificacao(),
                form.confidencialidade(), form.integridade(), form.disponibilidade());
    }

    private Usuario exigirEditor(Long solicitanteId) {
        Usuario usuario = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        if (usuario.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("O perfil Auditor tem acesso somente de leitura.");
        }
        return usuario;
    }

    private static void exigirEmUso(Ativo ativo) {
        if (!ativo.isEmUso()) {
            throw new RegraNegocioException("Ativo descontinuado não pode ser alterado.");
        }
    }

    private Ativo carregar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RegraNegocioException("Ativo não encontrado."));
    }

    /** Texto vazio ou só espaços vira null (não grava "" no banco). */
    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
