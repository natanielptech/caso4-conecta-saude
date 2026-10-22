package br.com.conectasaude.agendamento.acesso;

import java.util.UUID;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record UsuarioAutenticado(UUID id, String papel, UUID clinicaId) {
    public static UsuarioAutenticado de(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        UUID id = UUID.fromString(jwt.getSubject());
        String papel = jwt.getClaimAsStringList("roles").stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .filter(role -> role.equals("PACIENTE") || role.equals("CLINICA"))
                .findFirst()
                .orElse("");
        String clinica = jwt.getClaimAsString("clinic_id");
        return new UsuarioAutenticado(id, papel, clinica == null ? null : UUID.fromString(clinica));
    }
}
