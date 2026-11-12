package io.github.lucasolimpio.conformia.risco.service;

import io.github.lucasolimpio.conformia.risco.domain.Ameaca;
import io.github.lucasolimpio.conformia.risco.domain.Vulnerabilidade;
import io.github.lucasolimpio.conformia.risco.dto.ItemCatalogo;
import io.github.lucasolimpio.conformia.risco.repository.AmeacaRepository;
import io.github.lucasolimpio.conformia.risco.repository.VulnerabilidadeRepository;
import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import io.github.lucasolimpio.conformia.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Catálogos de ameaças e vulnerabilidades. Gerente e Analista incluem itens
 * novos pelo botão "+ Nova" do formulário de risco; nome não pode repetir
 * (ignorando maiúsculas).
 */
@Service
@Transactional
public class CatalogoRiscoService {

    private final AmeacaRepository ameacaRepository;
    private final VulnerabilidadeRepository vulnerabilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public CatalogoRiscoService(AmeacaRepository ameacaRepository, VulnerabilidadeRepository vulnerabilidadeRepository,
                                UsuarioRepository usuarioRepository, Clock clock) {
        this.ameacaRepository = ameacaRepository;
        this.vulnerabilidadeRepository = vulnerabilidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    public ItemCatalogo cadastrarAmeaca(String nome, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        String limpo = limpar(nome);
        if (ameacaRepository.existsByNomeIgnoreCase(limpo)) {
            throw new RegraNegocioException("nome", "Esta ameaça já existe no catálogo.");
        }
        Ameaca ameaca = ameacaRepository.save(new Ameaca(limpo, autor, clock.instant()));
        return new ItemCatalogo(ameaca.getId(), ameaca.getNome());
    }

    public ItemCatalogo cadastrarVulnerabilidade(String nome, Long solicitanteId) {
        Usuario autor = exigirEditor(solicitanteId);
        String limpo = limpar(nome);
        if (vulnerabilidadeRepository.existsByNomeIgnoreCase(limpo)) {
            throw new RegraNegocioException("nome", "Esta vulnerabilidade já existe no catálogo.");
        }
        Vulnerabilidade v = vulnerabilidadeRepository.save(new Vulnerabilidade(limpo, autor, clock.instant()));
        return new ItemCatalogo(v.getId(), v.getNome());
    }

    /** Espaços repetidos viram um só: "Falha   de  rede" = "Falha de rede". */
    private static String limpar(String nome) {
        String limpo = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3 || limpo.length() > 150) {
            throw new RegraNegocioException("nome", "O nome precisa ter entre 3 e 150 caracteres.");
        }
        return limpo;
    }

    private Usuario exigirEditor(Long solicitanteId) {
        Usuario usuario = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        if (usuario.getPerfil() == Perfil.AUDITOR) {
            throw new RegraNegocioException("O perfil Auditor tem acesso somente de leitura.");
        }
        return usuario;
    }
}
