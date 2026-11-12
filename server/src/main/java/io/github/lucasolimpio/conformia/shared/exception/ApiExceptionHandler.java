package io.github.lucasolimpio.conformia.shared.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros da API (@RestController) em JSON. Mesmo erro de negócio da web,
 * resposta adequada ao app.
 *
 * Ordem mais alta porque @RestController também é um @Controller: sem isso o
 * WebExceptionHandler poderia responder HTML para o app.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    /** 422: os dados vieram no formato certo, mas violam uma regra. */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroApi> regraNegocio(RegraNegocioException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErroApi("REGRA_NEGOCIO", e.getMessage(), e.getCampo(), null));
    }

    /** 400: falhou a validação de formato (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ErroApi("DADOS_INVALIDOS", "Confira os campos destacados.", null, campos));
    }

    /** 400: JSON malformado ou valor inválido (ex.: perfil inexistente). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApi> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErroApi.de("JSON_INVALIDO", "O corpo da requisição não é um JSON válido para esta operação."));
    }

    /** 401 no login. Mensagem genérica: não revela se o e-mail existe ou está inativo. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroApi> login(AuthenticationException e) {
        ErroApi erro = e instanceof CredentialsExpiredException
                ? ErroApi.de("SENHA_EXPIRADA", "Sua senha temporária expirou. Peça uma nova a um Gerente.")
                : ErroApi.de("CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }
}
