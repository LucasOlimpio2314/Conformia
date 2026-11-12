package io.github.lucasolimpio.conformia.aceite.domain;

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

/**
 * Aceite formal de um risco por um Gerente (ISO 27001 6.1.3 f).
 * Guarda uma "foto" do nível aceito: se o risco mudar, o aceite não vale mais.
 * Nunca é apagado: vence, é revogado ou invalidado — o histórico fica.
 */
@Entity
@Table(name = "aceite_risco")
public class AceiteRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risco_id", nullable = false, updatable = false)
    private Risco risco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoAceite tipo;

    @Column(nullable = false, updatable = false)
    private int probabilidade;

    @Column(nullable = false, updatable = false)
    private int impacto;

    @Column(nullable = false, length = 1000, updatable = false)
    private String justificativa;

    @Column(length = 1000, updatable = false)
    private String condicoes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aceito_por_id", nullable = false, updatable = false)
    private Usuario aceitoPor;

    @Column(name = "aceito_em", nullable = false, updatable = false)
    private Instant aceitoEm;

    @Column(name = "valido_ate", nullable = false, updatable = false)
    private Instant validoAte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoAceite situacao;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    /** null quando quem encerrou foi o sistema (vencimento). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encerrado_por_id")
    private Usuario encerradoPor;

    @Column(name = "motivo_encerramento", length = 500)
    private String motivoEncerramento;

    protected AceiteRisco() {
    }

    public AceiteRisco(Risco risco, TipoAceite tipo, int probabilidade, int impacto, String justificativa,
                       String condicoes, Usuario aceitoPor, Instant aceitoEm, Instant validoAte) {
        this.risco = risco;
        this.tipo = tipo;
        this.probabilidade = probabilidade;
        this.impacto = impacto;
        this.justificativa = justificativa;
        this.condicoes = condicoes;
        this.aceitoPor = aceitoPor;
        this.aceitoEm = aceitoEm;
        this.validoAte = validoAte;
        this.situacao = SituacaoAceite.VIGENTE;
    }

    /** Encerrado pelo sistema (sem usuário) quando passa da validade. */
    public void vencer(String motivo, Instant agora) {
        encerrar(SituacaoAceite.VENCIDO, null, motivo, agora);
    }

    public void revogar(Usuario gerente, String motivo, Instant agora) {
        encerrar(SituacaoAceite.REVOGADO, gerente, motivo, agora);
    }

    public void invalidar(Usuario quemAlterou, String motivo, Instant agora) {
        encerrar(SituacaoAceite.INVALIDADO, quemAlterou, motivo, agora);
    }

    private void encerrar(SituacaoAceite nova, Usuario quem, String motivo, Instant agora) {
        if (situacao != SituacaoAceite.VIGENTE) {
            throw new IllegalStateException("Aceite já não está vigente");
        }
        this.situacao = nova;
        this.encerradoPor = quem;
        this.motivoEncerramento = motivo;
        this.encerradoEm = agora;
    }

    public boolean isVigente() {
        return situacao == SituacaoAceite.VIGENTE;
    }

    public boolean venceuEm(Instant agora) {
        return isVigente() && agora.isAfter(validoAte);
    }

    public int getValor() {
        return probabilidade * impacto;
    }

    public Long getId() {
        return id;
    }

    public Risco getRisco() {
        return risco;
    }

    public TipoAceite getTipo() {
        return tipo;
    }

    public int getProbabilidade() {
        return probabilidade;
    }

    public int getImpacto() {
        return impacto;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public String getCondicoes() {
        return condicoes;
    }

    public Usuario getAceitoPor() {
        return aceitoPor;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }

    public Instant getValidoAte() {
        return validoAte;
    }

    public SituacaoAceite getSituacao() {
        return situacao;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    public String getMotivoEncerramento() {
        return motivoEncerramento;
    }
}
