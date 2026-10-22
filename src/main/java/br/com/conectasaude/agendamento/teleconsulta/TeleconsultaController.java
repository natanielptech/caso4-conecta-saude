package br.com.conectasaude.agendamento.teleconsulta;

import java.util.UUID;

import br.com.conectasaude.agendamento.acesso.PoliticaDeAcesso;
import br.com.conectasaude.agendamento.acesso.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v2")
public class TeleconsultaController {
    private final PoliticaDeAcesso politica;
    private final SalaTeleconsultaService salas;

    public TeleconsultaController(PoliticaDeAcesso politica, SalaTeleconsultaService salas) {
        this.politica = politica;
        this.salas = salas;
    }

    @PostMapping("/consultas/{id}/sala")
    public SalaTeleconsultaService.ConviteResponse criarSala(
            @PathVariable UUID id, JwtAuthenticationToken authentication) {
        var usuario = UsuarioAutenticado.de(authentication);
        var consulta = politica.exigirAcessoAConsulta(id, usuario);
        try {
            return salas.criar(consulta, usuario);
        } catch (SalaTeleconsultaService.SalaIndisponivelException exception) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        }
    }

    @PostMapping("/salas/acessar")
    public SalaTeleconsultaService.SalaResponse acessarSala(
            @RequestBody AcessarSalaRequest request, JwtAuthenticationToken authentication) {
        return salas.consumir(request.tokenUsoUnico(), UsuarioAutenticado.de(authentication));
    }

    public record AcessarSalaRequest(String tokenUsoUnico) {
    }
}
