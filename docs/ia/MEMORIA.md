# Memória do projeto

> Onde o projeto parou. Atualizar ao final de cada etapa (mais recente em cima).

## Estado atual

| | |
|---|---|
| Fase | Setup concluído — projeto sobe e conecta no PostgreSQL |
| Próximo passo | Módulo **usuário**: migration `V1__cria_usuario.sql`, entidade `Usuario` + enum `Perfil`, `SecurityConfig` com login, usuário inicial criado em Java (`ApplicationRunner`) com senha vinda do `.env` |

## Histórico

### 2026-11-05 — Testes configurados
- `ServerApplicationTests` renomeado para `ServerApplicationIT` com `@ActiveProfiles("test")` → roda no Failsafe contra `conformia_test`.
- Unitários (`*Test`) rodam com `mvnw test`; unitários + integração com `mvnw verify`.
- Testes de integração usam as mesmas credenciais do `.env` (o `conformia_app` é dono dos dois bancos).

### 2026-11-05 — Banco configurado
- PostgreSQL instalado; criados role `conformia_app` e bancos `conformia` e `conformia_test`.
- `server/.env` preenchido; projeto sobe e o Flyway criou a `flyway_schema_history` (nenhuma migration ainda).
- Por enquanto aparece o login padrão do Spring Security (usuário `user`, senha no console) e 404 depois — esperado, não há controllers.

### 2026-11-05 — Setup inicial
- Projeto Spring Boot 4.1.2 / Java 21 / Maven criado em `server/` (Initializr no IntelliJ).
- Repositório `github.com/LucasOlimpio2314/Conformia` criado (privado), primeiro commit enviado na `main`.
- Credenciais em `server/.env` lido via `spring.config.import`; `.env.example` versionado.
- Ajustes após revisão: driver e Flyway trocados de MySQL para PostgreSQL; `application.properties` com datasource, `ddl-auto=validate` e `open-in-view=false`; perfil de teste (`application-test.properties` → banco `conformia_test`); plugin Failsafe para testes `*IT`; `.gitignore` na raiz; criados `CLAUDE.md` e `docs/ia/`.

## Decisões

| Data | Decisão | Motivo |
|---|---|---|
| 2026-11-05 | Monorepo: `server/`, `mobile/`, `docs/` | Contrato da API e app mudam juntos; um lugar só para o grupo |
| 2026-11-05 | Web (Thymeleaf) e API no mesmo projeto `server` | Thymeleaf não tem build próprio; uma regra de negócio para os dois clientes |
| 2026-11-05 | Organização por módulo → camadas | Tudo de um assunto junto; facilita virar multi-módulo depois |
| 2026-11-05 | PostgreSQL (com pgAdmin) | Já usado no trabalho; roda em qualquer SO; bom com Spring |
| 2026-11-05 | Flyway para o schema | Banco igual na máquina de todo o grupo |
| 2026-11-05 | Migrations numeradas em sequência (`V1`, `V2`...), independentes da versão do sistema (`pom.xml`, semver + tag no Git) | Grupo cria migrations em paralelo sem disputar número de versão |
| 2026-11-05 | Usuário inicial criado em Java na inicialização, senha do `.env` | Não versionar senha padrão no repositório |
| 2026-11-05 | Testes no padrão AAA + nome de método por comportamento + AssertJ | Mais simples para o grupo aprender; o nome já dá a leitura de negócio. Migrar para Given-When-Then só se os testes virarem documentação para não-devs |
| 2026-11-05 | Repositório privado, sem licença por enquanto | Evitar exposição acidental; possível venda futura |

## Pendências

- [ ] Confirmar com o professor de banco se PostgreSQL é aceito.
- [x] Criar no PostgreSQL: role `conformia_app`, bancos `conformia` e `conformia_test` (owner `conformia_app`).
- [ ] Renomear `ServerApplication` → `ConformiaApplication` (opcional).
- [ ] Adicionar colegas como collaborators e proteger a `main`.
- [ ] Grupo definir versão do Flutter SDK; responsável do mobile cria `mobile/`.
