package io.github.lucasolimpio.conformia.inventario.domain;

import io.github.lucasolimpio.conformia.usuario.domain.Usuario;

/**
 * Dados editáveis de um ativo, já validados pelo service (proprietário
 * carregado do banco, textos limpos). Evita um construtor com 11 parâmetros.
 */
public record DadosAtivo(
        String nome,
        TipoAtivo tipo,
        String descricao,
        String localizacao,
        Usuario proprietario,
        boolean terceirizado,
        String fornecedor,
        Classificacao classificacao,
        int confidencialidade,
        int integridade,
        int disponibilidade
) {
}
