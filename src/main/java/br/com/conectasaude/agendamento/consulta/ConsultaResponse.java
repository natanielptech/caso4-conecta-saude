package br.com.conectasaude.agendamento.consulta;

import java.time.Instant;
import java.util.UUID;

public record ConsultaResponse(
        UUID id,
        String nomePaciente,
        Instant inicio,
        String status,
        String cpf,
        String cpfMascarado) {

    public static ConsultaResponse v1(Consulta consulta) {
        return new ConsultaResponse(
                consulta.getId(), consulta.getPaciente().getNome(), consulta.getInicio(),
                consulta.getStatus(), consulta.getPaciente().getCpf(), null);
    }

    public static ConsultaResponse v2(Consulta consulta) {
        String cpf = consulta.getPaciente().getCpf();
        String mascarado = "***.***.***-" + cpf.substring(9);
        return new ConsultaResponse(
                consulta.getId(), consulta.getPaciente().getNome(), consulta.getInicio(),
                consulta.getStatus(), null, mascarado);
    }
}
