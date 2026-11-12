package io.github.lucasolimpio.conformia.risco.dto;

import io.github.lucasolimpio.conformia.inventario.domain.Ativo;

/** Ativo ligado a um risco. emUso=false gera o aviso "ativo descontinuado" no risco. */
public record AtivoVinculado(Long id, String codigo, String nome, boolean emUso, int criticidade) {

    public static AtivoVinculado de(Ativo a) {
        return new AtivoVinculado(a.getId(), a.getCodigo(), a.getNome(), a.isEmUso(), a.getCriticidade().getNota());
    }
}
