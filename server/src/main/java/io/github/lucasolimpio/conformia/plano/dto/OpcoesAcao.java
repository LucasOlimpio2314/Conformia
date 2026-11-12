package io.github.lucasolimpio.conformia.plano.dto;

import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.risco.dto.ControleResumo;

import java.util.Arrays;
import java.util.List;

/** Opções do formulário da ação: responsáveis ativos e os controles DO RISCO. */
public record OpcoesAcao(List<ProprietarioOpcao> responsaveis, List<ControleResumo> controles,
                         List<SituacaoAcao> situacoes) {

    public OpcoesAcao(List<ProprietarioOpcao> responsaveis, List<ControleResumo> controles) {
        this(responsaveis, controles, Arrays.asList(SituacaoAcao.values()));
    }
}
