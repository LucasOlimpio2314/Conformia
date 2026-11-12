package io.github.lucasolimpio.conformia.risco.dto;

import io.github.lucasolimpio.conformia.inventario.dto.ProprietarioOpcao;
import io.github.lucasolimpio.conformia.risco.domain.EscalaRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;

import java.util.Arrays;
import java.util.List;

/**
 * Tudo que o formulário de risco precisa para montar os campos.
 * Na API: GET /api/riscos/opcoes (o app monta o formulário com isto).
 */
public record OpcoesRisco(
        List<AtivoOpcao> ativos,
        List<ItemCatalogo> ameacas,
        List<ItemCatalogo> vulnerabilidades,
        List<ControleResumo> controles,
        List<ProprietarioOpcao> donos,
        List<EscalaRisco.Nota> probabilidades,
        List<EscalaRisco.Nota> impactos,
        List<OpcaoTratamento> tratamentos
) {

    public OpcoesRisco(List<AtivoOpcao> ativos, List<ItemCatalogo> ameacas, List<ItemCatalogo> vulnerabilidades,
                       List<ControleResumo> controles, List<ProprietarioOpcao> donos) {
        this(ativos, ameacas, vulnerabilidades, controles, donos, EscalaRisco.probabilidades(),
                EscalaRisco.impactos(), Arrays.asList(OpcaoTratamento.values()));
    }
}
