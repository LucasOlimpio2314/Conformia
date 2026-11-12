package io.github.lucasolimpio.conformia.risco.dto;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;

/** Controle do Anexo A para tela e JSON (ex.: codigo "8.13", nome "Backup de informações"). */
public record ControleResumo(String codigo, String nome, int tema) {

    public static ControleResumo de(ControleAnexoA c) {
        return new ControleResumo(c.getCodigo(), c.getNome(), c.getTema());
    }

    /** "8.13 – Backup de informações" */
    public String rotulo() {
        return codigo + " – " + nome;
    }

    public String rotuloTema() {
        return ControleAnexoA.rotuloTema(tema);
    }
}
