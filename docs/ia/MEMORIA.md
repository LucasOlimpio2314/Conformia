# Memória do projeto

> Onde o projeto parou. Atualizar ao final de cada etapa (mais recente em cima).

## Estado atual

| | |
|---|---|
| Fase | Setup inicial do `server` |
| Próximo passo | Criar o banco no PostgreSQL e subir o projeto pela primeira vez; depois, módulo **usuário** (migration `V1__cria_usuario.sql`, entidade, login, usuário inicial) |

## Histórico

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
| 2026-11-05 | Repositório privado, sem licença por enquanto | Evitar exposição acidental; possível venda futura |

## Pendências

- [ ] Confirmar com o professor de banco se PostgreSQL é aceito.
- [ ] Criar no PostgreSQL: role `conformia_app`, bancos `conformia` e `conformia_test` (owner `conformia_app`).
- [ ] Renomear `ServerApplication` → `ConformiaApplication` (opcional).
- [ ] Adicionar colegas como collaborators e proteger a `main`.
- [ ] Grupo definir versão do Flutter SDK; responsável do mobile cria `mobile/`.
