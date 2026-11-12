package io.github.lucasolimpio.conformia.risco.dto;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.risco.domain.EscalaRisco;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import io.github.lucasolimpio.conformia.risco.domain.SituacaoRisco;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Risco para tela e JSON. Relacionamentos "achatados" em DTOs (nada de entidade). */
public record RiscoResumo(
        Long id,
        String codigo,
        String titulo,
        String cenario,
        ItemCatalogo ameaca,
        ItemCatalogo vulnerabilidade,
        List<AtivoVinculado> ativos,
        List<ControleResumo> controles,
        Long donoId,
        String donoNome,
        boolean donoAtivo,
        int probabilidade,
        int impacto,
        int valor,
        NivelRisco nivel,
        OpcaoTratamento tratamento,
        SituacaoRisco situacao,
        String justificativaEncerramento,
        Instant encerradoEm,
        Instant atualizadoEm,
        String atualizadoPorNome
) {

    public static RiscoResumo de(Risco r) {
        return new RiscoResumo(r.getId(), r.getCodigo(), r.getTitulo(), r.getCenario(),
                new ItemCatalogo(r.getAmeaca().getId(), r.getAmeaca().getNome()),
                new ItemCatalogo(r.getVulnerabilidade().getId(), r.getVulnerabilidade().getNome()),
                r.getAtivos().stream().map(AtivoVinculado::de)
                        .sorted(Comparator.comparing(AtivoVinculado::codigo)).toList(),
                r.getControles().stream()
                        .sorted(Comparator.comparingInt(ControleAnexoA::getTema).thenComparingInt(ControleAnexoA::getOrdem))
                        .map(ControleResumo::de).toList(),
                r.getDono().getId(), r.getDono().getNome(), r.getDono().isAtivo(),
                r.getProbabilidade(), r.getImpacto(), r.getValor(), r.getNivel(), r.getTratamento(),
                r.getSituacao(), r.getJustificativaEncerramento(), r.getEncerradoEm(),
                r.getAtualizadoEm(), r.getAtualizadoPor().getNome());
    }

    public boolean ativo() {
        return situacao == SituacaoRisco.ATIVO;
    }

    /** Aviso: algum ativo do risco foi descontinuado — revisar ou encerrar o risco. */
    public boolean temAtivoDescontinuado() {
        return ativo() && ativos.stream().anyMatch(a -> !a.emUso());
    }

    /** Aviso: o dono do risco foi desativado — escolher outro. */
    public boolean precisaNovoDono() {
        return ativo() && !donoAtivo;
    }

    /** Tudo que a busca da lista encontra: código, título, ameaça, vulnerabilidade, ativos e controles. */
    public String textoBusca() {
        StringBuilder texto = new StringBuilder(codigo).append(' ').append(titulo).append(' ')
                .append(ameaca.nome()).append(' ').append(vulnerabilidade.nome());
        ativos.forEach(a -> texto.append(' ').append(a.codigo()).append(' ').append(a.nome()));
        controles.forEach(c -> texto.append(' ').append(c.rotulo()));
        return texto.toString().toLowerCase();
    }

    public String rotuloProbabilidade() {
        return EscalaRisco.rotuloProbabilidade(probabilidade);
    }

    public String rotuloImpacto() {
        return EscalaRisco.rotuloImpacto(impacto);
    }

    public String iniciaisDono() {
        String[] partes = donoNome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }
}
