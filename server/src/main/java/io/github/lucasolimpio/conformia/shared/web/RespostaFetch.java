package io.github.lucasolimpio.conformia.shared.web;

import io.github.lucasolimpio.conformia.shared.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.BindingResult;

/**
 * Padrão das telas com drawer/modal (app.js):
 * - sucesso → cabeçalho X-Redirect + corpo vazio; o app.js navega;
 * - erro de formulário → status 422 + o fragmento do formulário com as mensagens.
 */
public final class RespostaFetch {

    public static final int STATUS_FORM_INVALIDO = 422;
    public static final String FRAGMENTO_VAZIO = "fragments/layout :: vazio";

    private RespostaFetch() {
    }

    public static String redirecionar(HttpServletResponse response, String url) {
        response.setHeader("X-Redirect", url);
        return FRAGMENTO_VAZIO;
    }

    /** Erro do service vai para o campo certo do formulário (ou para o topo, se não tiver campo). */
    public static void rejeitar(BindingResult erros, RegraNegocioException e) {
        if (e.getCampo() != null) {
            erros.rejectValue(e.getCampo(), "regra", e.getMessage());
        } else {
            erros.reject("regra", e.getMessage());
        }
    }

    /** Códigos como RSK-001 / ACT-012 vindos da URL (?salvo=) antes de irem para a tela. */
    public static boolean codigoValido(String codigo) {
        return codigo != null && codigo.matches("[A-Z]{3}-[A-Z0-9]{1,8}");
    }
}
