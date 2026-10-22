package br.com.conectasaude.agendamento.acesso;

import java.util.UUID;
import java.util.List;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record UsuarioAutenticado(UUID id, String papel, UUID clinicaId) {
    public static UsuarioAutenticado de(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        UUID id = UUID.fromString(jwt.getSubject());
        List<String> roles = jwt.getClaimAsStringList("roles");
        String papel = (roles == null ? List.<String>of() : roles).stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .filter(role -> role.equals("PACIENTE") || role.equals("CLINICA"))
                .findFirst()
                .orElse("");
        String clinica = jwt.getClaimAsString("clinic_id");
        return new UsuarioAutenticado(id, papel, clinica == null ? null : UUID.fromString(clinica));
    }
}
