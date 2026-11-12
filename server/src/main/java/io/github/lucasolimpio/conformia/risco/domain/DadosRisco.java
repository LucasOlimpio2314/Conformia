package io.github.lucasolimpio.conformia.risco.domain;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;

import java.util.Set;

/** Dados editáveis do risco, já validados e carregados do banco pelo RiscoService. */
public record DadosRisco(
        String titulo,
        String cenario,
        Set<Ativo> ativos,
        Ameaca ameaca,
        Vulnerabilidade vulnerabilidade,
        Set<ControleAnexoA> controles,
        Usuario dono,
        int probabilidade,
        int impacto,
        OpcaoTratamento tratamento
) {
}
