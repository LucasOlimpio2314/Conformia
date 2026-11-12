package io.github.lucasolimpio.conformia.aceite.dto;

import io.github.lucasolimpio.conformia.aceite.domain.AceiteRisco;
import io.github.lucasolimpio.conformia.aceite.domain.SituacaoAceite;
import io.github.lucasolimpio.conformia.aceite.domain.TipoAceite;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;

import java.time.Instant;

public record AceiteResumo(
        Long id,
        Long riscoId,
        String riscoCodigo,
        String riscoTitulo,
        TipoAceite tipo,
        int probabilidade,
        int impacto,
        int valor,
        NivelRisco nivel,
        String justificativa,
        String condicoes,
        String aceitoPorNome,
        Instant aceitoEm,
        Instant validoAte,
        SituacaoAceite situacao,
        Instant encerradoEm,
        String motivoEncerramento
) {

    public static AceiteResumo de(AceiteRisco a) {
        return new AceiteResumo(a.getId(), a.getRisco().getId(), a.getRisco().getCodigo(), a.getRisco().getTitulo(),
                a.getTipo(), a.getProbabilidade(), a.getImpacto(), a.getValor(), NivelRisco.de(a.getValor()),
                a.getJustificativa(), a.getCondicoes(), a.getAceitoPor().getNome(), a.getAceitoEm(), a.getValidoAte(),
                a.getSituacao(), a.getEncerradoEm(), a.getMotivoEncerramento());
    }

    public boolean vigente() {
        return situacao == SituacaoAceite.VIGENTE;
    }
}
