package io.github.lucasolimpio.conformia.risco.dto;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.risco.domain.OpcaoTratamento;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Comparator;
import java.util.List;

/**
 * Drawer do risco (novo e edição) e JSON da API. Aqui só formato;
 * regras (ativo em uso, dono válido, itens existentes) no RiscoService.
 */
public record RiscoForm(
        @NotBlank(message = "Informe o título.")
        @Size(min = 5, max = 200, message = "O título precisa ter entre 5 e 200 caracteres.")
        String titulo,

        @Size(max = 2000, message = "O cenário pode ter no máximo 2000 caracteres.")
        String cenario,

        @NotEmpty(message = "Escolha pelo menos um ativo.")
        List<Long> ativoIds,

        @NotNull(message = "Escolha a ameaça.")
        Long ameacaId,

        @NotNull(message = "Escolha a vulnerabilidade.")
        Long vulnerabilidadeId,

        @NotEmpty(message = "Escolha pelo menos um controle do Anexo A.")
        List<String> controles,

        @NotNull(message = "Escolha o dono do risco.")
        Long donoId,

        @NotNull(message = "Escolha a probabilidade.") @Min(1) @Max(5)
        Integer probabilidade,

        @NotNull(message = "Escolha o impacto.") @Min(1) @Max(5)
        Integer impacto,

        /** null = "a definir" */
        OpcaoTratamento tratamento
) {

    /** Lista ausente (nenhum checkbox marcado / campo fora do JSON) vira lista vazia. */
    public RiscoForm {
        ativoIds = ativoIds == null ? List.of() : List.copyOf(ativoIds);
        controles = controles == null ? List.of() : List.copyOf(controles);
    }

    public static RiscoForm vazio() {
        return new RiscoForm("", "", List.of(), null, null, List.of(), null, null, null, null);
    }

    /** Edição. Dono inativo vem vazio: é preciso escolher outro. */
    public static RiscoForm de(Risco r) {
        Long dono = r.getDono().isAtivo() ? r.getDono().getId() : null;
        return new RiscoForm(r.getTitulo(), r.getCenario(),
                r.getAtivos().stream().map(Ativo::getId).sorted().toList(),
                r.getAmeaca().getId(), r.getVulnerabilidade().getId(),
                r.getControles().stream()
                        .sorted(Comparator.comparingInt(ControleAnexoA::getTema).thenComparingInt(ControleAnexoA::getOrdem))
                        .map(ControleAnexoA::getCodigo).toList(),
                dono, r.getProbabilidade(), r.getImpacto(), r.getTratamento());
    }
}
