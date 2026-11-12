package io.github.lucasolimpio.conformia.aviso.dto;

import io.github.lucasolimpio.conformia.aviso.domain.Aviso;
import io.github.lucasolimpio.conformia.aviso.domain.TipoAviso;

import java.time.Instant;

public record AvisoResumo(Long id, TipoAviso tipo, String titulo, String mensagem, String link,
                          Instant criadoEm, boolean lido) {

    public static AvisoResumo de(Aviso a) {
        return new AvisoResumo(a.getId(), a.getTipo(), a.getTitulo(), a.getMensagem(), a.getLink(),
                a.getCriadoEm(), a.isLido());
    }
}
