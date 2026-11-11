# Memória do projeto

> Onde o projeto parou. Atualizar ao final de cada etapa (mais recente em cima).

## Estado atual

| | |
|---|---|
| Fase | Módulo **usuário** (web + API mobile) implementado na branch `feature/backend-usuario` (aguardando revisão/commit do Lucas) |
| Próximo passo | Lucas testar e commitar → PR. Depois: editar usuário (nome, perfil, ativar/desativar); responsável do mobile começa pelo login usando `docs/api/usuarios.md` |

## Histórico

### 2026-11-11 — API do mobile (auth e usuários)
- Login da API **por sessão** (cookie `JSESSIONID`), sem JWT. Segunda `SecurityFilterChain` só para `/api/**`: respostas 401/403 em JSON, CSRF desligado (cookie `SameSite=Lax` + só JSON + sem CORS).
- Endpoints em `usuario/api/`: `/api/auth` (login, me, trocar-senha, logout), `/api/minha-conta`, `/api/usuarios` — todos chamam o mesmo `UsuarioService` da web.
- `TrocaSenhaObrigatoriaFilter` responde `403 TROCA_SENHA_OBRIGATORIA` na API; `ApiExceptionHandler` + `ErroApi` padronizam os erros.
- Contrato para o Flutter em `docs/api/usuarios.md`.
- Testes: + `TrocaSenhaObrigatoriaFilterTest` (5) e `UsuarioApiIT` (13). Total: 27 unitários + 24 integração, `mvnw verify` verde.

### 2026-11-11 — Módulo usuário (login, troca de senha, lista e cadastro)
- Protótipo aprovado (artifact "Conformia Riskdesk — protótipo"): login, troca obrigatória, lista com filtros, drawer de cadastro, modal da senha temporária.
- `V1__cria_usuario.sql`: tabela `usuario` + usuário inicial GERENTE inserido com dados do `.env` (`ADMIN_NOME`, `ADMIN_EMAIL`, `ADMIN_SENHA`) via placeholders do Flyway; hash BCrypt gerado no SQL com `pgcrypto`; `ON CONFLICT (email) DO NOTHING`.
- Senha temporária: gerada pelo sistema (`GeradorSenha`, SecureRandom, 10 caracteres sem ambíguos), mostrada uma vez, vale 24h (usuário inicial: sem prazo). Login com ela → `/trocar-senha`; `TrocaSenhaObrigatoriaFilter` bloqueia as outras URLs até trocar.
- `/usuarios` só GERENTE (listar, criar, redefinir senha de outro usuário). "Minha conta" (clique no usuário no canto inferior esquerdo) para todos: ver dados e trocar a própria senha confirmando a atual.
- Regras de nova senha no `UsuarioService`: 8–64 caracteres, letras e números, confirmação igual, diferente da atual.
- Testes: 22 unitários (`UsuarioServiceTest`, `UsuarioTest`, `GeradorSenhaTest`) + 10 de integração (`UsuarioWebIT`). `mvnw verify` verde.
- Nesta etapa o Lucas pediu que a IA implementasse tudo, consultando só nas decisões.

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
| 2026-11-05 | ~~Usuário inicial criado em Java na inicialização~~ → substituída em 11/11 | — |
| 2026-11-11 | Usuário inicial inserido na `V1`, com nome/e-mail/senha do `.env` (placeholders Flyway) e `ON CONFLICT DO NOTHING` | Roda uma vez só (Flyway) e não versiona senha; o checksum da migration não muda se o `.env` mudar |
| 2026-11-11 | Senha temporária gerada pelo sistema, válida 24h, troca obrigatória no 1º login | Gerente nunca escolhe a senha de outra pessoa; senha perdida = nova temporária |
| 2026-11-11 | `/usuarios` só GERENTE; "Minha conta" para todos; só gerente gera temporária e nunca para si mesmo | Própria senha se troca confirmando a atual |
| 2026-11-11 | "Manter conectado" removido; sessão expira em 30 min | Menos superfície de ataque e menos segredo no `.env` |
| 2026-11-11 | API mobile autenticada por sessão (cookie), igual à web — sem JWT | Reaproveita o login existente; logout/bloqueio imediatos; menos conceitos para o grupo |
| 2026-11-05 | Testes no padrão AAA + nome de método por comportamento + AssertJ | Mais simples para o grupo aprender; o nome já dá a leitura de negócio. Migrar para Given-When-Then só se os testes virarem documentação para não-devs |
| 2026-11-05 | Repositório privado, sem licença por enquanto | Evitar exposição acidental; possível venda futura |

## Pendências

- [ ] Confirmar com o professor de banco se PostgreSQL é aceito.
- [x] Criar no PostgreSQL: role `conformia_app`, bancos `conformia` e `conformia_test` (owner `conformia_app`).
- [ ] Renomear `ServerApplication` → `ConformiaApplication` (opcional).
- [ ] Adicionar colegas como collaborators e proteger a `main`.
- [ ] Grupo definir versão do Flutter SDK; responsável do mobile cria `mobile/`.
- [ ] Cada colega precisa adicionar `ADMIN_NOME`, `ADMIN_EMAIL`, `ADMIN_SENHA` no próprio `server/.env` (ver `.env.example`).
- [ ] Mobile: guardar o cookie `JSESSIONID` no Flutter (pacote de cookies) — ver `docs/api/usuarios.md`.
- [ ] Editar usuário (nome, perfil, ativar/desativar) — botão "Editar" ainda não existe.
