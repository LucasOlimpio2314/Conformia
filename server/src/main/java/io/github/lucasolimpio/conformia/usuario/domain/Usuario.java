package io.github.lucasolimpio.conformia.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Usuário do sistema. Sem setters públicos: o estado só muda pelos métodos
 * de negócio abaixo, que garantem as regras da senha temporária.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "senha_temporaria", nullable = false)
    private boolean senhaTemporaria;

    @Column(name = "senha_expira_em")
    private Instant senhaExpiraEm;

    @Column(name = "ultimo_acesso")
    private Instant ultimoAcesso;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /** Exigido pelo JPA. */
    protected Usuario() {
    }

    /** Cria um usuário que já nasce com senha temporária. */
    public Usuario(String nome, String email, Perfil perfil, boolean ativo,
                   String hashSenhaTemporaria, Instant senhaExpiraEm, Instant criadoEm) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        definirSenhaTemporaria(hashSenhaTemporaria, senhaExpiraEm);
    }

    public void definirSenhaTemporaria(String hash, Instant expiraEm) {
        this.senhaHash = hash;
        this.senhaTemporaria = true;
        this.senhaExpiraEm = expiraEm;
    }

    public void definirSenhaPessoal(String hash) {
        this.senhaHash = hash;
        this.senhaTemporaria = false;
        this.senhaExpiraEm = null;
    }

    public void registrarAcesso(Instant quando) {
        this.ultimoAcesso = quando;
    }

    /** Só a senha temporária expira; sem prazo definido, nunca expira. */
    public boolean isSenhaExpirada(Instant agora) {
        return senhaTemporaria && senhaExpiraEm != null && agora.isAfter(senhaExpiraEm);
    }

    public boolean isGerente() {
        return perfil == Perfil.GERENTE;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isSenhaTemporaria() {
        return senhaTemporaria;
    }

    public Instant getSenhaExpiraEm() {
        return senhaExpiraEm;
    }

    public Instant getUltimoAcesso() {
        return ultimoAcesso;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
