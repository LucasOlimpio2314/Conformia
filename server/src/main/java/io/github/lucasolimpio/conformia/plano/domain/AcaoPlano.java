package io.github.lucasolimpio.conformia.plano.domain;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.risco.domain.Risco;
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
import java.time.LocalDate;

/**
 * Ação do plano de tratamento de um risco (ISO 27001 6.1.3 e, 8.3).
 * Concluída ou cancelada, não muda mais (vira registro para auditoria).
 */
@Entity
@Table(name = "acao_plano")
public class AcaoPlano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12, unique = true, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risco_id", nullable = false, updatable = false)
    private Risco risco;

    @Column(nullable = false, length = 200)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    /** LocalDate (sem hora): prazo é um dia do calendário. */
    @Column(nullable = false)
    private LocalDate prazo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "controle_codigo", nullable = false)
    private ControleAnexoA controle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoAcao situacao;

    @Column(length = 1000)
    private String evidencia;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

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

    protected AcaoPlano() {
    }

    public AcaoPlano(String codigo, Risco risco, DadosAcao dados, Usuario autor, Instant agora) {
        this.codigo = codigo;
        this.risco = risco;
        this.situacao = SituacaoAcao.PENDENTE;
        this.criadoEm = agora;
        this.criadoPor = autor;
        aplicar(dados, autor, agora);
    }

    public void atualizar(DadosAcao dados, Usuario autor, Instant agora) {
        if (situacao.isFinalizada()) {
            throw new IllegalStateException("Ação finalizada não pode ser alterada");
        }
        aplicar(dados, autor, agora);
    }

    private void aplicar(DadosAcao d, Usuario autor, Instant agora) {
        if (d.situacao() == SituacaoAcao.CONCLUIDA && (d.evidencia() == null || d.evidencia().isBlank())) {
            throw new IllegalArgumentException("Concluir exige evidência");
        }
        this.titulo = d.titulo();
        this.responsavel = d.responsavel();
        this.prazo = d.prazo();
        this.controle = d.controle();
        this.situacao = d.situacao();
        this.evidencia = d.evidencia();
        this.finalizadaEm = d.situacao().isFinalizada() ? agora : null;
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    /** Em aberto e com o prazo já passado. */
    public boolean isAtrasada(LocalDate hoje) {
        return !situacao.isFinalizada() && prazo.isBefore(hoje);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Risco getRisco() {
        return risco;
    }

    public String getTitulo() {
        return titulo;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public ControleAnexoA getControle() {
        return controle;
    }

    public SituacaoAcao getSituacao() {
        return situacao;
    }

    public String getEvidencia() {
        return evidencia;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Usuario getAtualizadoPor() {
        return atualizadoPor;
    }
}
