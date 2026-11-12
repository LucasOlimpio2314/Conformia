package io.github.lucasolimpio.conformia.plano.domain;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;

import java.time.LocalDate;

/** Dados editáveis da ação, já validados e carregados pelo PlanoAcaoService. */
public record DadosAcao(String titulo, Usuario responsavel, LocalDate prazo, ControleAnexoA controle,
                        SituacaoAcao situacao, String evidencia) {
}
