package io.github.lucasolimpio.conformia.plano.dto;

import io.github.lucasolimpio.conformia.aceite.dto.AceiteResumo;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;

import java.util.List;

/**
 * Plano de um risco: inerente → residual esperado, ações e progresso.
 * {@code aceiteVigente} só vem preenchido quando o residual já foi aceito.
 */
public record PlanoRisco(
        Long riscoId,
        String codigo,
        String titulo,
        OpcaoTratamento tratamento,
        int probabilidade,
        int impacto,
        int valor,
        NivelRisco nivel,
        Integer probabilidadeResidual,
        Integer impactoResidual,
        Integer valorResidual,
        NivelRisco nivelResidual,
        List<AcaoResumo> acoes,
        int concluidas,
        int validas,
        boolean prontoParaAceite,
        AceiteResumo aceiteVigente
) {

    public boolean temResidual() {
        return probabilidadeResidual != null;
    }

    /** Percentual da barra de progresso (canceladas não contam). */
    public int percentual() {
        return validas == 0 ? 0 : concluidas * 100 / validas;
    }

    public boolean aguardandoAceite() {
        return prontoParaAceite && aceiteVigente == null;
    }
}
