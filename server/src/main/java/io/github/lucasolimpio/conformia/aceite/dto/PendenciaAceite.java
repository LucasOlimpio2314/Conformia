package io.github.lucasolimpio.conformia.aceite.dto;

import io.github.lucasolimpio.conformia.aceite.domain.TipoAceite;
import io.github.lucasolimpio.conformia.risco.domain.NivelRisco;
import io.github.lucasolimpio.conformia.risco.dto.RiscoResumo;

/**
 * Risco aguardando aceite do Gerente.
 * {@code nivel} = o que será aceito (inerente se RISCO, residual se RESIDUAL).
 * {@code anterior} = último aceite que deixou de valer (para explicar por que voltou).
 */
public record PendenciaAceite(
        RiscoResumo risco,
        TipoAceite tipo,
        int probabilidade,
        int impacto,
        int valor,
        NivelRisco nivel,
        int acoesConcluidas,
        AceiteResumo anterior
) {
}
