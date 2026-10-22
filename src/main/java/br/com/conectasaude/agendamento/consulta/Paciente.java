package br.com.conectasaude.agendamento.consulta;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pacientes")
public class Paciente {
    @Id
    private UUID id;

    @Column(name = "nome_civil", nullable = false)
    private String nomeCivil;

    @Column(name = "nome_social")
    private String nomeSocial;

    @Column(nullable = false, length = 11)
    private String cpf;

    protected Paciente() {
    }

    public Paciente(UUID id, String nomeCivil, String nomeSocial, String cpf) {
        this.id = id;
        this.nomeCivil = nomeCivil;
        this.nomeSocial = nomeSocial;
        this.cpf = cpf;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nomeSocial == null || nomeSocial.isBlank() ? nomeCivil : nomeSocial;
    }

    public String getCpf() {
        return cpf;
    }
}
