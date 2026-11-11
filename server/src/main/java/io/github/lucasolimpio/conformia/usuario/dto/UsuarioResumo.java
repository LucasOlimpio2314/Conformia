package io.github.lucasolimpio.conformia.usuario.dto;

import io.github.lucasolimpio.conformia.usuario.domain.Perfil;
import io.github.lucasolimpio.conformia.usuario.domain.Usuario;

import java.time.Instant;

/** Dados do usuário que podem ir para a tela. Sem hash de senha. */
public record UsuarioResumo(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        boolean senhaTemporaria,
        Instant ultimoAcesso,
        Instant criadoEm
) {

    public static UsuarioResumo de(Usuario u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                u.isSenhaTemporaria(), u.getUltimoAcesso(), u.getCriadoEm());
    }

    public Situacao situacao() {
        if (!ativo) {
            return Situacao.INATIVO;
        }
        if (senhaTemporaria) {
            return ultimoAcesso == null ? Situacao.AGUARDANDO_PRIMEIRO_ACESSO : Situacao.SENHA_REDEFINIDA;
        }
        return Situacao.ATIVO;
    }

    public String iniciais() {
        String[] partes = nome.trim().split("\\s+");
        String iniciais = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciais += partes[partes.length - 1].substring(0, 1);
        }
        return iniciais.toUpperCase();
    }

    public String primeiroNome() {
        return nome.trim().split("\\s+")[0];
    }

    public enum Situacao {
        ATIVO("Ativo", "ativo"),
        AGUARDANDO_PRIMEIRO_ACESSO("Aguardando 1º acesso", "pendente"),
        SENHA_REDEFINIDA("Senha redefinida", "pendente"),
        INATIVO("Inativo", "inativo");

        private final String rotulo;
        private final String css;

        Situacao(String rotulo, String css) {
            this.rotulo = rotulo;
            this.css = css;
        }

        public String getRotulo() {
            return rotulo;
        }

        public String getCss() {
            return css;
        }
    }
}
