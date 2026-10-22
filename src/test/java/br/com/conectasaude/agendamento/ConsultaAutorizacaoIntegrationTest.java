package br.com.conectasaude.agendamento;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID pacienteA = UUID.randomUUID();
    private final UUID pacienteB = UUID.randomUUID();
    private final UUID consultaB = UUID.randomUUID();
    private final UUID consultaA = UUID.randomUUID();
    private final UUID clinicaA = UUID.randomUUID();

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
        jdbc.update(
                "INSERT INTO consultas (id, paciente_id, clinica_id, inicio, status) VALUES (?, ?, ?, ?, ?)",
                consultaA, pacienteA, clinicaA, Instant.now(), "AGENDADA");
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

    @Test
    void versoesRetornamCpfCorretoEHeadersDeDeprecacao() throws Exception {
        var jwt = jwt().jwt(token -> token
                .subject(pacienteA.toString())
                .claim("roles", List.of("PACIENTE"))
                .issuer("https://auth.conectasaude.local")
                .audience(List.of("agendamento-service")));

        mockMvc.perform(get("/v1/consultas/{id}", consultaA).with(jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Fri, 31 Dec 2027 23:59:59 GMT"))
                .andExpect(jsonPath("$.cpf").value("11111111111"))
                .andExpect(jsonPath("$.cpfMascarado").doesNotExist());

        mockMvc.perform(get("/v2/consultas/{id}", consultaA).with(jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpfMascarado").value("***.***.***-11"))
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void conviteDeTeleconsultaVinculadoAoPacienteSoPodeSerUsadoUmaVez() throws Exception {
        var jwt = jwt().jwt(token -> token
                .subject(pacienteA.toString())
                .claim("roles", List.of("PACIENTE"))
                .issuer("https://auth.conectasaude.local")
                .audience(List.of("agendamento-service")));
        String convite = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .post("/v2/consultas/{id}/sala", consultaA).with(jwt).with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(convite).get("tokenUsoUnico").asText();
        String request = objectMapper.writeValueAsString(java.util.Map.of("tokenUsoUnico", token));

        var acesso = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/v2/salas/acessar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .with(jwt)
                .with(csrf());
        mockMvc.perform(acesso).andExpect(status().isOk());
        mockMvc.perform(acesso).andExpect(status().isNotFound());
    }
}
