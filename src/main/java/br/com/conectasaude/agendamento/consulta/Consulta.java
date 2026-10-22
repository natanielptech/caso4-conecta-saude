package br.com.conectasaude.agendamento.consulta;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "consultas")
public class Consulta {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Column(nullable = false, length = 30)
    private String status;

    protected Consulta() {
    }

    public Consulta(UUID id, Paciente paciente, UUID clinicaId, Instant inicio, String status) {
        this.id = id;
        this.paciente = paciente;
        this.clinicaId = clinicaId;
        this.inicio = inicio;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public UUID getClinicaId() {
        return clinicaId;
    }

    public Instant getInicio() {
        return inicio;
    }

    public String getStatus() {
        return status;
    }
}
