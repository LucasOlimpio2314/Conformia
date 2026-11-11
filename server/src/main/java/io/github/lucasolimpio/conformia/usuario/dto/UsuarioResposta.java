package io.github.lucasolimpio.conformia.usuario.dto;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;

import java.time.Instant;

/**
 * Usuário em JSON para o app. Datas saem em ISO-8601 UTC
 * (ex.: "2026-11-11T12:00:00Z"); o app converte para o fuso local.
 */
public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        UsuarioResumo.Situacao situacao,
        Instant ultimoAcesso,
        Instant criadoEm
) {

    public static UsuarioResposta de(UsuarioResumo u) {
        return new UsuarioResposta(u.id(), u.nome(), u.email(), u.perfil(), u.ativo(), u.situacao(),
                u.ultimoAcesso(), u.criadoEm());
    }
}
