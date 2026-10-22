package br.com.conectasaude.agendamento.auditoria;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class ServicoAuditoria {
    private final AuditoriaRepository repository;

    public ServicoAuditoria(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuarioId, String recurso, boolean permitido) {
        repository.save(new Auditoria(
                usuarioId == null ? "anonimo" : usuarioId,
                recurso,
                permitido ? "PERMITIDO" : "NEGADO",
                Instant.now()));
    }
}
