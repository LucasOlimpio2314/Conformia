package io.github.lucasolimpio.conformia.inventario.dto;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.Criticidade;
import io.github.lucasolimpio.conformia.inventario.domain.SituacaoAtivo;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;

import java.time.Instant;

/** Ativo para tela e JSON. Dados do proprietário "achatados" (sem a entidade Usuario). */
public record AtivoResumo(
        Long id,
        String codigo,
        String nome,
        TipoAtivo tipo,
        String descricao,
        String localizacao,
        Long proprietarioId,
        String proprietarioNome,
        boolean proprietarioAtivo,
        boolean terceirizado,
        String fornecedor,
        Classificacao classificacao,
        int confidencialidade,
        int integridade,
        int disponibilidade,
        Criticidade criticidade,
        SituacaoAtivo situacao,
        String justificativaDescontinuacao,
        Instant descontinuadoEm,
        Instant atualizadoEm,
        String atualizadoPorNome
) {

    public static AtivoResumo de(Ativo a) {
        return new AtivoResumo(a.getId(), a.getCodigo(), a.getNome(), a.getTipo(), a.getDescricao(),
                a.getLocalizacao(), a.getProprietario().getId(), a.getProprietario().getNome(),
                a.getProprietario().isAtivo(), a.isTerceirizado(), a.getFornecedor(), a.getClassificacao(),
                a.getConfidencialidade(), a.getIntegridade(), a.getDisponibilidade(), a.getCriticidade(),
                a.getSituacao(), a.getJustificativaDescontinuacao(), a.getDescontinuadoEm(),
                a.getAtualizadoEm(), a.getAtualizadoPor().getNome());
    }

    public boolean emUso() {
        return situacao == SituacaoAtivo.EM_USO;
    }

    /** Aviso na lista: ativo em uso cujo proprietário foi desativado. */
    public boolean precisaNovoProprietario() {
        return emUso() && !proprietarioAtivo;
    }

    public String iniciaisProprietario() {
        String[] partes = proprietarioNome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }
}
