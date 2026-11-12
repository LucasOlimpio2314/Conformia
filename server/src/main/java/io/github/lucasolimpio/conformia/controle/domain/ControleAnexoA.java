package io.github.lucasolimpio.conformia.controle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Controle do Anexo A da ISO/IEC 27001:2022 (ex.: 8.13 – Backup de informações).
 * Catálogo fixo carregado pela migration V3: o sistema só lê.
 * O futuro módulo "Controles do Anexo A" (declaração de aplicabilidade) parte daqui.
 */
@Entity
@Table(name = "controle_anexo_a")
public class ControleAnexoA {

    /** Número do controle na norma, ex.: "8.13". */
    @Id
    @Column(length = 5)
    private String codigo;

    /** 5 Organizacionais, 6 Pessoas, 7 Físicos, 8 Tecnológicos. */
    @Column(nullable = false)
    private int tema;

    /** Número depois do ponto (8.13 → 13): ordena 8.2 antes de 8.10. */
    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, length = 150)
    private String nome;

    protected ControleAnexoA() {
    }

    public String getCodigo() {
        return codigo;
    }

    public int getTema() {
        return tema;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getNome() {
        return nome;
    }

    public static String rotuloTema(int tema) {
        return switch (tema) {
            case 5 -> "Organizacionais";
            case 6 -> "Pessoas";
            case 7 -> "Físicos";
            case 8 -> "Tecnológicos";
            default -> "Tema " + tema;
        };
    }
}
