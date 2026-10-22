package br.com.conectasaude.agendamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import br.com.conectasaude.agendamento.consulta.Consulta;
import br.com.conectasaude.agendamento.consulta.ConsultaResponse;
import br.com.conectasaude.agendamento.consulta.Paciente;
import org.junit.jupiter.api.Test;

class ConsultaResponseTest {
    @Test
    void v2RetornaSomenteCpfMascarado() {
        var paciente = new Paciente(UUID.randomUUID(), "Nome", null, "12345678901");
        var consulta = new Consulta(UUID.randomUUID(), paciente, UUID.randomUUID(), Instant.now(), "AGENDADA");

        var resposta = ConsultaResponse.v2(consulta);

        assertEquals("***.***.***-01", resposta.cpfMascarado());
        assertNull(resposta.cpf());
    }
}
