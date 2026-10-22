package br.com.conectasaude.agendamento.consulta;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {
}
