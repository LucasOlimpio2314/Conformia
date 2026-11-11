package io.github.lucasolimpio.conformia.usuario.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Gera senhas temporárias. SecureRandom (e não Random) porque o valor
 * precisa ser imprevisível. Sem caracteres ambíguos (0/O, 1/l/I) para
 * a pessoa não errar ao digitar o que recebeu.
 */
@Component
public class GeradorSenha {

    static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    static final int TAMANHO = 10;

    private final SecureRandom random = new SecureRandom();

    public String gerar() {
        StringBuilder senha = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            senha.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
