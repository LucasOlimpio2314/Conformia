package io.github.lucasolimpio.conformia.inventario.dto;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;

/** Item da lista "Proprietário" (só Gerentes e Analistas ativos). */
public record ProprietarioOpcao(Long id, String nome, Perfil perfil) {
}
