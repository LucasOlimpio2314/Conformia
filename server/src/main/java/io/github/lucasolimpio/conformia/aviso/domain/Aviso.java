package io.github.lucasolimpio.conformia.aviso.domain;

import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Aviso dentro do sistema para um usuário (hoje: Gerentes).
 * Pensado para virar também e-mail no futuro sem mudar quem cria o aviso.
 */
@Entity
@Table(name = "aviso")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false, updatable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAviso tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    /** Tela que resolve o aviso, ex.: "/aceites". */
    @Column(nullable = false, length = 200)
    private String link;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "lido_em")
    private Instant lidoEm;

    protected Aviso() {
    }

    public Aviso(Usuario destinatario, TipoAviso tipo, String titulo, String mensagem, String link, Instant criadoEm) {
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.link = link;
        this.criadoEm = criadoEm;
    }

    public void marcarLido(Instant quando) {
        if (lidoEm == null) {
            this.lidoEm = quando;
        }
    }

    public boolean isLido() {
        return lidoEm != null;
    }

    public Long getId() {
        return id;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public TipoAviso getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getLink() {
        return link;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getLidoEm() {
        return lidoEm;
    }
}
