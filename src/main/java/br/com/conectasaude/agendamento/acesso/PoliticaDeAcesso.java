package br.com.conectasaude.agendamento.acesso;

import java.util.UUID;

import br.com.conectasaude.agendamento.auditoria.ServicoAuditoria;
import br.com.conectasaude.agendamento.consulta.Consulta;
import br.com.conectasaude.agendamento.consulta.ConsultaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PoliticaDeAcesso {
    private final ConsultaRepository consultas;
    private final ServicoAuditoria auditoria;

    public PoliticaDeAcesso(ConsultaRepository consultas, ServicoAuditoria auditoria) {
        this.consultas = consultas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Consulta exigirAcessoAConsulta(UUID consultaId, UsuarioAutenticado usuario) {
        Consulta consulta = consultas.findWithPacienteById(consultaId).orElse(null);
        boolean permitido = consulta != null && possuiAcesso(consulta, usuario);
        auditoria.registrar(usuario.id().toString(), "consulta:" + consultaId, permitido);
        if (!permitido) {
            throw new RecursoNaoEncontradoException();
        }
        return consulta;
    }

    private boolean possuiAcesso(Consulta consulta, UsuarioAutenticado usuario) {
        return switch (usuario.papel()) {
            case "PACIENTE" -> consulta.getPaciente().getId().equals(usuario.id());
            case "CLINICA" -> usuario.clinicaId() != null && consulta.getClinicaId().equals(usuario.clinicaId());
            default -> false;
        };
    }
}
