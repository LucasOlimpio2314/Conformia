package io.github.lucasolimpio.conformia.plano.domain;

import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;

import java.util.List;
import java.util.Set;

/**
 * Regras do plano usadas por mais de um módulo (plano e aceite).
 * Classe só com funções puras: fácil de testar e sem depender do Spring.
 */
public final class RegrasPlano {

    /** Tratamentos que pedem plano de ação (o "Aceitar" vai direto para o aceite). */
    public static final Set<OpcaoTratamento> TRATAMENTOS_COM_PLANO =
            Set.of(OpcaoTratamento.MITIGAR, OpcaoTratamento.TRANSFERIR, OpcaoTratamento.EVITAR);

    private RegrasPlano() {
    }

    public static boolean temPlano(Risco risco) {
        return risco.isAtivo() && risco.getTratamento() != null && TRATAMENTOS_COM_PLANO.contains(risco.getTratamento());
    }

    /**
     * O residual pode ir para aceite quando: o risco tem plano, o residual
     * esperado está definido e todas as ações (sem contar canceladas) foram concluídas.
     */
    public static boolean prontoParaAceite(Risco risco, List<AcaoPlano> acoes) {
        List<AcaoPlano> validas = acoes.stream().filter(a -> a.getSituacao() != SituacaoAcao.CANCELADA).toList();
        return temPlano(risco) && risco.temResidual() && !validas.isEmpty()
                && validas.stream().allMatch(a -> a.getSituacao() == SituacaoAcao.CONCLUIDA);
    }
}
