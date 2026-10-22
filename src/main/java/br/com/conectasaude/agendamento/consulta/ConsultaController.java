package br.com.conectasaude.agendamento.consulta;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import br.com.conectasaude.agendamento.acesso.PoliticaDeAcesso;
import br.com.conectasaude.agendamento.acesso.UsuarioAutenticado;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ConsultaController {
    private static final String SUNSET = "2027-12-31T23:59:59Z";
    private final PoliticaDeAcesso politica;

    public ConsultaController(PoliticaDeAcesso politica) {
        this.politica = politica;
    }

    @GetMapping("/v1/consultas/{id}")
    public ResponseEntity<ConsultaResponse> obterV1(
            @PathVariable UUID id, JwtAuthenticationToken authentication) {
        Consulta consulta = politica.exigirAcessoAConsulta(id, UsuarioAutenticado.de(authentication));
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Sunset", DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        ZonedDateTime.parse(SUNSET).withZoneSameInstant(java.time.ZoneOffset.UTC)))
                .body(ConsultaResponse.v1(consulta));
    }

    @GetMapping("/v2/consultas/{id}")
    public ConsultaResponse obterV2(
            @PathVariable UUID id, JwtAuthenticationToken authentication) {
        Consulta consulta = politica.exigirAcessoAConsulta(id, UsuarioAutenticado.de(authentication));
        return ConsultaResponse.v2(consulta);
    }
}
