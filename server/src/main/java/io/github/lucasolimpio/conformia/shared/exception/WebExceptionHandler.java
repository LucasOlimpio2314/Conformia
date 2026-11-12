package io.github.lucasolimpio.conformia.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Erros de negócio que o controller não tratou, nas telas (@Controller).
 * - Requisição do app.js (fetch): responde só o texto, que vira um toast.
 * - Navegação normal: página de erro com a mensagem.
 * A API (@RestController) terá o seu ApiExceptionHandler, respondendo JSON.
 */
@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ModelAndView regraNegocio(RegraNegocioException e, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        if ("fetch".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(422);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(e.getMessage());
            return null; // resposta já escrita
        }
        ModelAndView mav = new ModelAndView("erro");
        mav.addObject("mensagem", e.getMessage());
        mav.setStatus(HttpStatusCode.valueOf(422));
        return mav;
    }
}
