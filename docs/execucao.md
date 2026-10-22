# Executar o serviço

Requisitos: Java 17+ e PostgreSQL 14+. As migrações Flyway rodam no início da aplicação.

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5432/conecta_saude
export DATABASE_USERNAME=conecta
export DATABASE_PASSWORD='senha-local'
export JWT_JWK_SET_URI=https://idp.exemplo/.well-known/jwks.json
export JWT_ISSUER=https://idp.exemplo/
export JWT_AUDIENCE=agendamento-service
mvn spring-boot:run
```

O provedor JWT precisa emitir `sub` com o UUID do usuário, `roles` com `PACIENTE` ou `CLINICA` e, para clínica, `clinic_id`. O serviço valida assinatura via JWKS, emissor, público, validade e duração máxima de 15 minutos. Configure TLS no ingress/proxy para todas as conexões externas e internas.

- `GET /v1/consultas/{id}`: consulta autorizada com CPF completo e headers `Deprecation`/`Sunset`.
- `GET /v2/consultas/{id}`: consulta autorizada com CPF mascarado.
- `POST /v2/consultas/{id}/sala`: convite temporário de uso único; desabilitado por padrão.
- `POST /v2/salas/acessar`: resgata convite com o mesmo JWT do paciente.
- `/swagger-ui.html`: documentação OpenAPI.
- `GET /csrf`: retorna o token CSRF; inclua `X-CSRF-TOKEN` nas requisições de alteração (POST/PUT/PATCH/DELETE).

Use `FEATURE_TELECONSULTA_ENABLED=true` para habilitar a funcionalidade. Os convites deste protótipo ficam em memória; em implantação com múltiplas instâncias, substitua o armazenamento por mecanismo compartilhado com consumo atômico.

O ciclo de esquema começa com `V0__schema_inicial.sql`; `V1` expande, `V2` migra dados em lotes de 1.000, a versão atual do código lê os novos campos e `V3` contrai após a janela de reversão. A implantação por etapas deve ocorrer em releases coordenadas, mantendo a versão anterior até a cópia e validação dos dados; restaure backup para reversão após a migração de dados. `V4` instala a proteção append-only da auditoria.
