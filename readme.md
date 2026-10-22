# ConectaSaúde — Segurança, Testes e Evolução de APIs e Serviços

> Este README é a especificação funcional e arquitetural para implementação de uma aplicação baseada no estudo de caso da Aula 4 (Unidade 4). Ele **não contém código** — a implementação deve ser feita por outra IA/desenvolvedor a partir destas instruções.

## Stack obrigatória

- **Linguagem: Java** (17 ou 21 LTS)
- **Framework: Spring Boot 3.x**, com **Spring Security** (OAuth2 Resource Server / validação de JWT)
- Build: Maven ou Gradle
- Migrações de banco: **Flyway** ou Liquibase (obrigatório para o padrão expandir/contrair)
- Documentação de API: springdoc-openapi
- Testes: JUnit 5 + Spring Boot Test + Testcontainers (integração); REST Assured ou MockMvc (contrato/e2e); ArchUnit é opcional aqui
- Auditoria: uma tabela/log de auditoria **append-only** (nunca UPDATE/DELETE)

## Contexto de negócio

A ConectaSaúde conecta pacientes a cerca de 2.400 clínicas. Pacientes agendam consultas e teleconsultas e acessam orientações/prescrições pelo aplicativo; clínicas se integram via API pública B2B. Por lidar com dados de saúde (LGPD), a plataforma é auditada periodicamente.

**O achado que motiva o caso:** um teste de intrusão revelou uma falha de **Broken Object Level Authorization (BOLA)** — um paciente autenticado, ao trocar o identificador na URL (`GET /v1/consultas/88213` → `/v1/consultas/88214`), conseguia ver dados de consulta de **outro paciente**. O token era validado (autenticação), mas não havia verificação de que o usuário tinha direito àquele objeto específico (autorização).

## Serviço a construir

**agendamento-service** — serviço principal, onde todos os requisitos abaixo devem ser implementados. Os serviços de Prontuário e Faturamento (mencionados no caso) podem ser representados como stubs simples ou omitidos, desde que o foco de Agendamento esteja completo.

## Requisito de segurança central — Autorização em nível de objeto (obrigatório, não negociável)

Todo endpoint que recebe um identificador de recurso deve:

1. Obter o identificador do **usuário autenticado a partir do token** (JWT), nunca de um parâmetro enviado pelo cliente
2. Verificar se o usuário tem direito **àquele objeto específico** (ex. `consulta.pacienteId == usuarioDoToken.id`, ou `consulta.clinicaId == usuarioDoToken.clinicaId` para usuários de clínica)
3. Se não tiver direito, responder **`404 Not Found`** (não `403 Forbidden`) — para não revelar a existência do recurso a quem não tem acesso a ele
4. Registrar a tentativa negada na auditoria

Esta regra deve ser implementada de forma **centralizada** (ex. um componente `PoliticaDeAcesso` reutilizado por todos os endpoints), não repetida manualmente em cada controller.

## Modelagem de ameaças (STRIDE) — referência para o fluxo de teleconsulta

| Categoria | Ameaça | Mitigação esperada |
|---|---|---|
| Falsificação (Spoofing) | Terceiro entra na sala de vídeo com link vazado | Link de uso único, vinculado ao token do paciente |
| Adulteração (Tampering) | Alteração da prescrição em trânsito | TLS em toda a comunicação + assinatura digital da prescrição |
| Repúdio (Repudiation) | Profissional nega ter emitido uma prescrição | Registro de auditoria imutável, com autor e horário |
| Divulgação (Information disclosure) | Gravação da consulta acessível por URL pública | Armazenamento privado + URLs assinadas com expiração curta |
| Negação de serviço | Excesso de salas abertas por um único cliente | Limite de sessões simultâneas por clínica |
| Elevação de privilégio | Paciente aciona endpoint exclusivo de profissionais | Verificação de papel (role) por rota |

## Riscos do OWASP API Security Top 10 (2023) a mitigar

| Risco | Mitigação obrigatória no projeto |
|---|---|
| API1 Broken Object Level Authorization | Ver seção "Autorização em nível de objeto" acima |
| API2 Broken Authentication | Tokens JWT com expiração curta e validação de assinatura/emissor/público |
| API3 Broken Object Property Level Authorization | DTOs de resposta explícitos, nunca devolver a entidade completa (ex. nunca expor CPF completo — ver seção de evolução de API) |
| API4 Unrestricted Resource Consumption | Rate limiting básico por cliente/token |
| API5 Broken Function Level Authorization | Endpoints administrativos protegidos por verificação de escopo/role |
| API9 Improper Inventory Management | Versões antigas de API devem ser rastreadas e desligadas (ver seção de evolução) |

## Pirâmide de testes exigida

| Camada | O que verifica | Ferramenta sugerida |
|---|---|---|
| Unitário | Regras isoladas (ex. a lógica de `PoliticaDeAcesso`) | JUnit 5 |
| Integração | Serviço com banco real | `@SpringBootTest` + Testcontainers |
| Contrato | Compatibilidade entre consumidor e provedor | Pact ou Spring Cloud Contract |
| Ponta a ponta | Poucos fluxos críticos completos | REST Assured / MockMvc |

**Teste de regressão de segurança obrigatório:** criar dois pacientes de teste; confirmar que o paciente A, autenticado, recebe `404` ao tentar `GET /v1/consultas/{id}` de uma consulta do paciente B, e que a tentativa gera um registro de auditoria.

## Evolução de API — obrigatório

- Implementar **v1** (expõe `cpf` completo — já marcada como obsoleta) e **v2** (expõe `cpfMascarado`), **coexistindo**
- Respostas da v1 devem incluir os headers HTTP `Deprecation` e `Sunset`
- **Feature flag** para liberar uma nova funcionalidade (ex. sala de teleconsulta) gradualmente — pode ser implementada com uma configuração simples (ex. propriedade de aplicação lida em runtime) ou uma biblioteca de feature flags; toda flag deve ter um responsável e uma data de remoção documentados
- **Padrão expandir e contrair** (expand/contract) para uma mudança de esquema incompatível — usar como exercício a separação do campo `nome` em `nome_social` e `nome_civil`, implementada em **4 migrações Flyway separadas**:

| Etapa | Script | O que muda | Versões que funcionam |
|---|---|---|---|
| 1. Expandir | `V1__expandir_nome.sql` | Cria as novas colunas; aplicação grava nas antigas e nas novas | Antiga e nova |
| 2. Migrar | `V2__migrar_nome.sql` | Copia registros existentes para as novas colunas em lotes | Antiga e nova |
| 3. Alternar | (mudança de código, não SQL) | Aplicação passa a ler apenas das novas colunas | Só a nova |
| 4. Contrair | `V3__remover_nome_antigo.sql` | Remove as colunas antigas, após a janela de reversão | Só a nova |

## Auditoria e ADR — obrigatório

- Tabela de auditoria imutável: `quem` (id do usuário), `o quê` (recurso acessado), `quando` (timestamp), `resultado` (permitido/negado)
- Incluir no repositório um arquivo `docs/adr/ADR-001-autorizacao-nivel-objeto.md` documentando a decisão de autorização em nível de objeto: contexto, decisão, alternativas rejeitadas (ex. "regra só no gateway" — rejeitada porque o gateway não conhece a relação usuário-dado) e consequências

## Critérios de aceite

- [ ] Teste de regressão de BOLA presente e passando (paciente A não acessa consulta de paciente B → `404` + auditoria registrada)
- [ ] v1 retorna `cpf` com headers `Deprecation`/`Sunset`; v2 retorna `cpfMascarado`
- [ ] Pelo menos uma feature flag implementada com responsável/data de remoção documentados
- [ ] As 4 etapas de expand/contract implementadas como migrações Flyway distintas e reversíveis
- [ ] Tabela de auditoria populada em cada leitura/alteração de dado sensível
- [ ] ADR documentado em `docs/adr/`
