package br.com.conectasaude.agendamento;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MigracaoPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("conecta")
            .withUsername("conecta")
            .withPassword("conecta");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void auditoriaRejeitaAtualizacoes() {
        jdbc.update("INSERT INTO auditoria (quem, recurso, resultado, quando) VALUES ('teste', 'consulta', 'PERMITIDO', CURRENT_TIMESTAMP)");
        boolean rejeitou = false;
        try {
            jdbc.update("UPDATE auditoria SET resultado = 'NEGADO' WHERE quem = 'teste'");
        } catch (RuntimeException exception) {
            rejeitou = true;
        }
        assertTrue(rejeitou);
    }
}
