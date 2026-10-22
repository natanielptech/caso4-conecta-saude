package br.com.conectasaude.agendamento.auditoria;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String quem;

    @Column(nullable = false, length = 200)
    private String recurso;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(nullable = false)
    private Instant quando;

    protected Auditoria() {
    }

    public Auditoria(String quem, String recurso, String resultado, Instant quando) {
        this.quem = quem;
        this.recurso = recurso;
        this.resultado = resultado;
        this.quando = quando;
    }
}
