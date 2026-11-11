package io.github.lucasolimpio.conformia.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Formato único de erro da API:
 * {"codigo": "REGRA_NEGOCIO", "mensagem": "...", "campo": "email", "campos": {...}}
 * - codigo:   para o app decidir o que fazer (ex.: TROCA_SENHA_OBRIGATORIA abre a troca)
 * - mensagem: texto pronto para mostrar ao usuário
 * - campo / campos: só quando o erro é de um campo do formulário
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroApi(String codigo, String mensagem, String campo, Map<String, String> campos) {

    public static ErroApi de(String codigo, String mensagem) {
        return new ErroApi(codigo, mensagem, null, null);
    }
}
