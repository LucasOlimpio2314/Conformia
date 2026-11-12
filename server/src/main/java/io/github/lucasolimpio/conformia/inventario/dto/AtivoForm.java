package io.github.lucasolimpio.conformia.inventario.dto;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.inventario.domain.Classificacao;
import io.github.lucasolimpio.conformia.inventario.domain.TipoAtivo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados do drawer de ativo (novo e edição) e do JSON da API.
 * Aqui só formato; regras (proprietário válido, fornecedor obrigatório) no AtivoService.
 * Notas como Integer para detectar "não informado" (int viraria 0 em silêncio).
 */
public record AtivoForm(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 3, max = 150, message = "O nome precisa ter entre 3 e 150 caracteres.")
        String nome,

        @NotNull(message = "Escolha o tipo.")
        TipoAtivo tipo,

        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres.")
        String descricao,

        @Size(max = 150, message = "A localização pode ter no máximo 150 caracteres.")
        String localizacao,

        @NotNull(message = "Escolha o proprietário.")
        Long proprietarioId,

        /** Boolean (não boolean): omitido no JSON = não terceirizado. Ver construtor abaixo. */
        Boolean terceirizado,

        @Size(max = 120, message = "O fornecedor pode ter no máximo 120 caracteres.")
        String fornecedor,

        @NotNull(message = "Escolha a classificação.")
        Classificacao classificacao,

        @NotNull(message = "Dê uma nota de 1 a 5.") @Min(1) @Max(5)
        Integer confidencialidade,

        @NotNull(message = "Dê uma nota de 1 a 5.") @Min(1) @Max(5)
        Integer integridade,

        @NotNull(message = "Dê uma nota de 1 a 5.") @Min(1) @Max(5)
        Integer disponibilidade
) {

    /**
     * Construtor compacto do record: roda antes de guardar os campos.
     * O Jackson 3 recusa o JSON inteiro se um boolean primitivo faltar;
     * com Boolean, ausente chega null e aqui vira false.
     */
    public AtivoForm {
        terceirizado = Boolean.TRUE.equals(terceirizado);
    }

    public static AtivoForm vazio() {
        return new AtivoForm("", TipoAtivo.INFORMACAO, "", "", null, false, "",
                Classificacao.INTERNA, 3, 3, 3);
    }

    /** Preenche o formulário de edição. Proprietário inativo vem vazio: é preciso escolher outro. */
    public static AtivoForm de(Ativo a) {
        Long proprietario = a.getProprietario().isAtivo() ? a.getProprietario().getId() : null;
        return new AtivoForm(a.getNome(), a.getTipo(), a.getDescricao(), a.getLocalizacao(), proprietario,
                a.isTerceirizado(), a.getFornecedor(), a.getClassificacao(),
                a.getConfidencialidade(), a.getIntegridade(), a.getDisponibilidade());
    }
}
