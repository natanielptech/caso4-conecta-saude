package br.com.conectasaude.agendamento.teleconsulta;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import br.com.conectasaude.agendamento.auditoria.ServicoAuditoria;
import br.com.conectasaude.agendamento.acesso.RecursoNaoEncontradoException;
import br.com.conectasaude.agendamento.acesso.UsuarioAutenticado;
import br.com.conectasaude.agendamento.consulta.Consulta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SalaTeleconsultaService {
    private static final long EXPIRACAO_SEGUNDOS = 300;
    private final Map<String, Convite> convites = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock = Clock.systemUTC();
    private final ServicoAuditoria auditoria;
    private final boolean habilitada;
    private final int maximoPorClinica;

    public SalaTeleconsultaService(
            ServicoAuditoria auditoria,
            @Value("${feature.teleconsulta.enabled:false}") boolean habilitada,
            @Value("${feature.teleconsulta.max-sessoes-por-clinica:3}") int maximoPorClinica) {
        this.auditoria = auditoria;
        this.habilitada = habilitada;
        this.maximoPorClinica = maximoPorClinica;
    }

    public synchronized ConviteResponse criar(Consulta consulta, UsuarioAutenticado usuario) {
        if (!habilitada || !"PACIENTE".equals(usuario.papel())) {
            auditoria.registrar(usuario.id().toString(), "teleconsulta:" + consulta.getId(), false);
            throw new RecursoNaoEncontradoException();
        }
        limparExpirados();
        long sessoesAbertas = convites.values().stream()
                .filter(convite -> convite.clinicaId().equals(consulta.getClinicaId()))
                .count();
        if (sessoesAbertas >= maximoPorClinica) {
            auditoria.registrar(usuario.id().toString(), "teleconsulta:" + consulta.getId(), false);
            throw new SalaIndisponivelException();
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiraEm = clock.instant().plusSeconds(EXPIRACAO_SEGUNDOS);
        convites.put(token, new Convite(usuario.id(), consulta.getClinicaId(), consulta.getId(), expiraEm));
        auditoria.registrar(usuario.id().toString(), "teleconsulta:" + consulta.getId(), true);
        return new ConviteResponse(token, expiraEm);
    }

    public synchronized SalaResponse consumir(String token, UsuarioAutenticado usuario) {
        limparExpirados();
        Convite convite = convites.get(token);
        boolean permitido = convite != null && convite.pacienteId().equals(usuario.id())
                && "PACIENTE".equals(usuario.papel());
        auditoria.registrar(
                usuario.id().toString(),
                "teleconsulta:" + (convite == null ? "sala" : convite.consultaId()),
                permitido);
        if (!permitido) {
            throw new RecursoNaoEncontradoException();
        }
        convites.remove(token);
        return new SalaResponse(convite.consultaId(), convite.expiraEm());
    }

    private void limparExpirados() {
        Instant agora = clock.instant();
        Iterator<Convite> iterator = convites.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().expiraEm().isAfter(agora)) {
                iterator.remove();
            }
        }
    }

    private record Convite(UUID pacienteId, UUID clinicaId, UUID consultaId, Instant expiraEm) {
    }

    public record ConviteResponse(String tokenUsoUnico, Instant expiraEm) {
    }

    public record SalaResponse(UUID consultaId, Instant expiraEm) {
    }

    public static class SalaIndisponivelException extends RuntimeException {
        public SalaIndisponivelException() {
            super("Limite de salas simultâneas atingido");
        }
    }
}
