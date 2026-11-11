package io.github.lucasolimpio.conformia.shared.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Erros de segurança da API saem dos filtros, antes de chegar a um controller,
 * então o ApiExceptionHandler não os pega. Aqui o JSON é escrito na mão,
 * no mesmo formato do ErroApi: {"codigo": "...", "mensagem": "..."}.
 * As mensagens são fixas do sistema (sem aspas), então não precisa de escape.
 */
final class RespostaJson {

    private RespostaJson() {
    }

    static void escrever(HttpServletResponse response, int status, String codigo, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensagem\":\"" + mensagem + "\"}");
    }
}
