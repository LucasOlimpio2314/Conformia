package io.github.lucasolimpio.conformia.plano.dto;

import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import io.github.lucasolimpio.conformia.risco.dto.ControleResumo;

import java.time.Instant;
import java.time.LocalDate;

public record AcaoResumo(
        Long id,
        String codigo,
        Long riscoId,
        String titulo,
        Long responsavelId,
        String responsavelNome,
        boolean responsavelAtivo,
        LocalDate prazo,
        ControleResumo controle,
        SituacaoAcao situacao,
        String evidencia,
        boolean atrasada,
        Instant finalizadaEm,
        Instant atualizadoEm,
        String atualizadoPorNome
) {

    public static AcaoResumo de(AcaoPlano a, LocalDate hoje) {
        return new AcaoResumo(a.getId(), a.getCodigo(), a.getRisco().getId(), a.getTitulo(),
                a.getResponsavel().getId(), a.getResponsavel().getNome(), a.getResponsavel().isAtivo(),
                a.getPrazo(), ControleResumo.de(a.getControle()), a.getSituacao(), a.getEvidencia(),
                a.isAtrasada(hoje), a.getFinalizadaEm(), a.getAtualizadoEm(), a.getAtualizadoPor().getNome());
    }

    public boolean editavel() {
        return !situacao.isFinalizada();
    }

    public String iniciaisResponsavel() {
        String[] partes = responsavelNome.trim().split("\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }
}
