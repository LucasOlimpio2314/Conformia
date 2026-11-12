package io.github.lucasolimpio.conformia.risco.domain;

import io.github.lucasolimpio.conformia.controle.domain.ControleAnexoA;
import io.github.lucasolimpio.conformia.inventario.domain.Ativo;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Risco de segurança da informação sobre um ou mais ativos.
 * Risco inerente = probabilidade × impacto (matriz 5x5). Sem setters:
 * muda só por {@link #atualizar} e {@link #encerrar}, que registram quem alterou.
 *
 * Set (e não List) nas coleções: o Hibernate consegue buscar as duas
 * de uma vez na listagem sem o erro "MultipleBagFetchException".
 */
@Entity
@Table(name = "risco")
public class Risco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12, unique = true, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String cenario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "risco_ativo",
            joinColumns = @JoinColumn(name = "risco_id"),
            inverseJoinColumns = @JoinColumn(name = "ativo_id"))
    private Set<Ativo> ativos = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ameaca_id", nullable = false)
    private Ameaca ameaca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vulnerabilidade_id", nullable = false)
    private Vulnerabilidade vulnerabilidade;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "risco_controle",
            joinColumns = @JoinColumn(name = "risco_id"),
            inverseJoinColumns = @JoinColumn(name = "controle_codigo"))
    private Set<ControleAnexoA> controles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dono_id", nullable = false)
    private Usuario dono;

    @Column(nullable = false)
    private int probabilidade;

    @Column(nullable = false)
    private int impacto;

    /** null = "a definir". */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OpcaoTratamento tratamento;

    /** Risco residual esperado depois do plano de ação (os dois null = não definido). */
    @Column(name = "probabilidade_residual")
    private Integer probabilidadeResidual;

    @Column(name = "impacto_residual")
    private Integer impactoResidual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoRisco situacao;

    @Column(name = "justificativa_encerramento", length = 500)
    private String justificativaEncerramento;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;

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
    protected Risco() {
    }

    public Risco(String codigo, DadosRisco dados, Usuario autor, Instant agora) {
        this.codigo = codigo;
        this.situacao = SituacaoRisco.ATIVO;
        this.criadoEm = agora;
        this.criadoPor = autor;
        aplicar(dados, autor, agora);
    }

    public void atualizar(DadosRisco dados, Usuario autor, Instant agora) {
        if (!isAtivo()) {
            throw new IllegalStateException("Risco encerrado não pode ser alterado");
        }
        aplicar(dados, autor, agora);
    }

    public void encerrar(String justificativa, Usuario autor, Instant agora) {
        if (!isAtivo()) {
            throw new IllegalStateException("Risco já está encerrado");
        }
        this.situacao = SituacaoRisco.ENCERRADO;
        this.justificativaEncerramento = justificativa;
        this.encerradoEm = agora;
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    private void aplicar(DadosRisco d, Usuario autor, Instant agora) {
        this.titulo = d.titulo();
        this.cenario = d.cenario();
        // troca o conteúdo (não a referência) para o Hibernate atualizar só as linhas que mudaram
        this.ativos.clear();
        this.ativos.addAll(d.ativos());
        this.ameaca = d.ameaca();
        this.vulnerabilidade = d.vulnerabilidade();
        this.controles.clear();
        this.controles.addAll(d.controles());
        this.dono = d.dono();
        this.probabilidade = d.probabilidade();
        this.impacto = d.impacto();
        this.tratamento = d.tratamento();
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    /**
     * Como o risco deve ficar depois das ações do plano. Não pode ser pior
     * que o risco inerente (o plano existe para reduzir).
     */
    public void definirResidual(int probabilidade, int impacto, Usuario autor, Instant agora) {
        if (!isAtivo()) {
            throw new IllegalStateException("Risco encerrado não pode ser alterado");
        }
        if (probabilidade * impacto > getValor()) {
            throw new IllegalArgumentException("Residual maior que o risco inerente");
        }
        this.probabilidadeResidual = probabilidade;
        this.impactoResidual = impacto;
        this.atualizadoEm = agora;
        this.atualizadoPor = autor;
    }

    public boolean temResidual() {
        return probabilidadeResidual != null;
    }

    /** Valor do risco residual esperado, ou null se não definido. */
    public Integer getValorResidual() {
        return temResidual() ? probabilidadeResidual * impactoResidual : null;
    }

    public Integer getProbabilidadeResidual() {
        return probabilidadeResidual;
    }

    public Integer getImpactoResidual() {
        return impactoResidual;
    }

    /** Valor do risco inerente na matriz 5x5 (1 a 25). */
    public int getValor() {
        return probabilidade * impacto;
    }

    public NivelRisco getNivel() {
        return NivelRisco.de(getValor());
    }

    public boolean isAtivo() {
        return situacao == SituacaoRisco.ATIVO;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCenario() {
        return cenario;
    }

    public Set<Ativo> getAtivos() {
        return Set.copyOf(ativos);
    }

    public Ameaca getAmeaca() {
        return ameaca;
    }

    public Vulnerabilidade getVulnerabilidade() {
        return vulnerabilidade;
    }

    public Set<ControleAnexoA> getControles() {
        return Set.copyOf(controles);
    }

    public Usuario getDono() {
        return dono;
    }

    public int getProbabilidade() {
        return probabilidade;
    }

    public int getImpacto() {
        return impacto;
    }

    public OpcaoTratamento getTratamento() {
        return tratamento;
    }

    public SituacaoRisco getSituacao() {
        return situacao;
    }

    public String getJustificativaEncerramento() {
        return justificativaEncerramento;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
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
