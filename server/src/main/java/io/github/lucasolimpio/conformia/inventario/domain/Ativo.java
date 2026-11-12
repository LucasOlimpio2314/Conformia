package io.github.lucasolimpio.conformia.inventario.domain;

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
 * Ativo do inventário. Sem setters: muda só por {@link #atualizar} e
 * {@link #descontinuar}, que mantêm as regras (terceirizado exige fornecedor,
 * descontinuado não é editado) e registram quem alterou.
 */
@Entity
@Table(name = "ativo")
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12, unique = true, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAtivo tipo;

    @Column(length = 1000)
    private String descricao;

    @Column(length = 150)
    private String localizacao;

    /** LAZY: só busca o usuário no banco se alguém usar o proprietário. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Usuario proprietario;

    @Column(nullable = false)
    private boolean terceirizado;

    @Column(length = 120)
    private String fornecedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Classificacao classificacao;

    @Column(nullable = false)
    private int confidencialidade;

    @Column(nullable = false)
    private int integridade;

    @Column(nullable = false)
    private int disponibilidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoAtivo situacao;

    @Column(name = "justificativa_descontinuacao", length = 500)
    private String justificativaDescontinuacao;

    @Column(name = "descontinuado_em")
    private Instant descontinuadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false, updatable = false)
    private Usuario criadoPor;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atualizado_por_id", nullable = false)
    private Usuario atualizadoPor;

    /** Exigido pelo JPA. */
    protected Ativo() {
    }

    public Ativo(String codigo, DadosAtivo dados, Usuario autor, Instant agora) {
        this.codigo = codigo;
        this.situacao = SituacaoAtivo.EM_USO;
        this.criadoEm = agora;
        this.criadoPor = autor;
        aplicar(dados, autor, agora);
    }

    public void atualizar(DadosAtivo dados, Usuario autor, Instant agora) {
        if (situacao == SituacaoAtivo.DESCONTINUADO) {
            throw new IllegalStateException("Ativo descontinuado não pode ser alterado");
        }
        aplicar(dados, autor, agora);
    }

    public void descontinuar(String justificativa, Usuario autor, Instant agora) {
        if (situacao == SituacaoAtivo.DESCONTINUADO) {
            throw new IllegalStateException("Ativo já está descontinuado");
        }
        this.situacao = SituacaoAtivo.DESCONTINUADO;
        this.justificativaDescontinuacao = justificativa;
        this.descontinuadoEm = agora;
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    private void aplicar(DadosAtivo d, Usuario autor, Instant agora) {
        this.nome = d.nome();
        this.tipo = d.tipo();
        this.descricao = d.descricao();
        this.localizacao = d.localizacao();
        this.proprietario = d.proprietario();
        this.terceirizado = d.terceirizado();
        this.fornecedor = d.terceirizado() ? d.fornecedor() : null;
        this.classificacao = d.classificacao();
        this.confidencialidade = d.confidencialidade();
        this.integridade = d.integridade();
        this.disponibilidade = d.disponibilidade();
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    /** Maior nota entre C, I e D: o pior dano possível define o quanto o ativo é crítico. */
    public Criticidade getCriticidade() {
        return Criticidade.daNota(Math.max(confidencialidade, Math.max(integridade, disponibilidade)));
    }

    public boolean isEmUso() {
        return situacao == SituacaoAtivo.EM_USO;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public TipoAtivo getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public Usuario getProprietario() {
        return proprietario;
    }

    public boolean isTerceirizado() {
        return terceirizado;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public Classificacao getClassificacao() {
        return classificacao;
    }

    public int getConfidencialidade() {
        return confidencialidade;
    }

    public int getIntegridade() {
        return integridade;
    }

    public int getDisponibilidade() {
        return disponibilidade;
    }

    public SituacaoAtivo getSituacao() {
        return situacao;
    }

    public String getJustificativaDescontinuacao() {
        return justificativaDescontinuacao;
    }

    public Instant getDescontinuadoEm() {
        return descontinuadoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Usuario getCriadoPor() {
        return criadoPor;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Usuario getAtualizadoPor() {
        return atualizadoPor;
    }
}
