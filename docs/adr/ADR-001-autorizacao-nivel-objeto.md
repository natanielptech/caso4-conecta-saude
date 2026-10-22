# ADR-001: Autorização em nível de objeto

- **Status:** Aceita
- **Data:** 2026-10-22

## Contexto

O serviço trata dados de saúde e recebeu um achado BOLA: um paciente autenticado podia trocar o identificador da consulta na URL e obter dados de outra pessoa. Validar assinatura, emissor e público do JWT comprova a identidade do usuário, mas não seu direito sobre cada consulta.

## Decisão

Toda leitura de consulta passa pela componente central `PoliticaDeAcesso`. A identidade e o papel são extraídos exclusivamente das claims assinadas do JWT. Pacientes só acessam consultas ligadas ao seu próprio identificador; usuários de clínica só acessam consultas da clínica indicada no token. Recursos inexistentes e não autorizados têm a mesma resposta `404`, e ambos os resultados de autorização são registrados na tabela append-only `auditoria`.

## Alternativas rejeitadas

- **Validar somente no gateway:** rejeitada porque o gateway não conhece a relação entre usuário, paciente e consulta.
- **Repetir verificações nos controllers:** rejeitada por aumentar o risco de um endpoint novo esquecer a autorização.
- **Responder `403` para recursos existentes:** rejeitada porque revela a existência de dados a usuários sem acesso.

## Consequências

- Cada endpoint que recebe um identificador de consulta deve usar `PoliticaDeAcesso`.
- As claims JWT de identidade, papel e clínica devem ser emitidas por um provedor confiável.
- Tentativas negadas ficam disponíveis para auditoria sem expor ao cliente se o identificador existe.
- A integração deve manter validação de emissor, público, assinatura e validade do token, além de TLS entre serviços.
