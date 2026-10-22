package br.com.conectasaude.agendamento;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultaAutorizacaoIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID pacienteA = UUID.randomUUID();
    private final UUID pacienteB = UUID.randomUUID();
    private final UUID consultaB = UUID.randomUUID();

    @BeforeEach
    void prepararDados() {
        jdbc.update("DELETE FROM auditoria");
        jdbc.update("DELETE FROM consultas");
        jdbc.update("DELETE FROM pacientes");
        jdbc.update(
                "INSERT INTO pacientes (id, cpf, nome_civil) VALUES (?, ?, ?)",
                pacienteA, "11111111111", "Paciente A");
        jdbc.update(
                "INSERT INTO pacientes (id, cpf, nome_civil) VALUES (?, ?, ?)",
                pacienteB, "22222222222", "Paciente B");
        jdbc.update(
                "INSERT INTO consultas (id, paciente_id, clinica_id, inicio, status) VALUES (?, ?, ?, ?, ?)",
                consultaB, pacienteB, UUID.randomUUID(), Instant.now(), "AGENDADA");
    }

    @Test
    void pacienteNaoAcessaConsultaDeOutroPacienteEtentativaFicaAuditada() throws Exception {
        mockMvc.perform(get("/v1/consultas/{id}", consultaB)
                        .with(jwt().jwt(token -> token
                                .subject(pacienteA.toString())
                                .claim("roles", List.of("PACIENTE"))
                                .issuer("https://auth.conectasaude.local")
                                .audience(List.of("agendamento-service")))))
                .andExpect(status().isNotFound());

        Integer registrosNegados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditoria WHERE quem = ? AND recurso = ? AND resultado = 'NEGADO'",
                Integer.class,
                pacienteA.toString(),
                "consulta:" + consultaB);
        assertEquals(1, registrosNegados);
    }
}
