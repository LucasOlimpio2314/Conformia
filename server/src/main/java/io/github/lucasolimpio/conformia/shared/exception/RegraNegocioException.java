package io.github.lucasolimpio.conformia.shared.exception;

/**
 * Violação de regra de negócio. A mensagem é mostrada ao usuário, então
 * deve dizer o que aconteceu e como resolver.
 *
 * {@code campo} é opcional: quando preenchido, a tela mostra o erro
 * junto do campo do formulário em vez de no topo.
 */
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
