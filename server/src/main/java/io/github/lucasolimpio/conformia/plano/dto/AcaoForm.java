package io.github.lucasolimpio.conformia.plano.dto;

import io.github.lucasolimpio.conformia.plano.domain.AcaoPlano;
import io.github.lucasolimpio.conformia.plano.domain.SituacaoAcao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Drawer da ação (nova e atualizar) e JSON da API.
 * Evidência obrigatória ao concluir é regra do service (depende da situação).
 */
public record AcaoForm(
        @NotBlank(message = "Descreva a ação.")
        @Size(min = 5, max = 200, message = "A ação precisa ter entre 5 e 200 caracteres.")
        String titulo,

        @NotNull(message = "Escolha o responsável.")
        Long responsavelId,

        /** ISO (2026-12-31): é o formato do <input type="date"> e do JSON. */
        @NotNull(message = "Informe o prazo.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate prazo,

        @NotBlank(message = "Escolha o controle do Anexo A.")
        String controle,

        @NotNull(message = "Escolha a situação.")
        SituacaoAcao situacao,

        @Size(max = 1000, message = "A evidência pode ter no máximo 1000 caracteres.")
        String evidencia
) {

    /** Ação nova: pendente, sugerindo o primeiro controle do risco. */
    public static AcaoForm vazio(String primeiroControle) {
        return new AcaoForm("", null, null, primeiroControle, SituacaoAcao.PENDENTE, "");
    }

    /** Responsável desativado vem vazio: é preciso escolher outro. */
    public static AcaoForm de(AcaoPlano a) {
        Long responsavel = a.getResponsavel().isAtivo() ? a.getResponsavel().getId() : null;
        return new AcaoForm(a.getTitulo(), responsavel, a.getPrazo(), a.getControle().getCodigo(),
                a.getSituacao(), a.getEvidencia());
    }
}
