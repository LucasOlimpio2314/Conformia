package io.github.lucasolimpio.conformia.risco.domain;

import io.github.lucasolimpio.conformia.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Item do catálogo de vulnerabilidades (a fraqueza que a ameaça explora). */
@Entity
@Table(name = "vulnerabilidade")
public class Vulnerabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** null = item inicial, criado pela migration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", updatable = false)
    private Usuario criadoPor;

    protected Vulnerabilidade() {
    }

    public Vulnerabilidade(String nome, Usuario criadoPor, Instant criadoEm) {
        this.nome = nome;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
