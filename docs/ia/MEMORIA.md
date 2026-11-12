# Memória do projeto

> Onde o projeto parou. Atualizar ao final de cada etapa (mais recente em cima).

## Estado atual

| | |
|---|---|
| Fase | Módulo **inventário de ativos** (web + API) implementado na branch `feature/backend-inventario` (criada a partir da `feature/backend-usuario`), aguardando commit do Lucas |
| Próximo passo | Lucas commitar o inventário (`C:\Faculdade\commit-modulo-inventario.bat`) e abrir PR. Depois: **cadastro de risco** (matriz 5×5, ativo escolhido do inventário, impacto sugerido pela criticidade C-I-D) |

## Histórico

### 2026-11-11 — E2E no Chrome + edição/ativação de usuários
- E2E manual no Chrome (localhost:8080) cobrindo login, senha temporária, troca obrigatória (web e API), bloqueios por perfil, inventário (cadastro, erro de fornecedor, edição, descontinuar), Minha conta e tentativas do Auditor de alterar pela URL/API. Dados de teste no banco de desenvolvimento: usuários "E2E Analista" (desativado) e "E2E Auditor", ativo ATV-002 (descontinuado).
- **Bug encontrado**: a chave (switch) aparecia desligada mesmo marcada — o `th:field` põe um `<input hidden name="_campo">` entre o checkbox e o `<i>`, e o CSS usava `+`. Corrigido para `~`. Era o motivo do usuário "Lucas" ter sido criado inativo.
- Novo: Gerente **edita nome/e-mail** e **ativa/desativa** usuário (drawer "Editar" + `PUT /api/usuarios/{id}`). Regras: não pode desativar a si mesmo; e-mail não repete; editar a própria conta recarrega a sessão (`SessaoUsuario`) para o menu mostrar o nome novo.
- Novo: `UsuarioAtivoFilter` — desativado perde a sessão aberta na próxima requisição (web → `/login?desativado`, API → `401 USUARIO_INATIVO`).
- Corrigido: `POST /api/usuarios` sem `ativo` (agora omitido = ativo).
- Testes: 54 unitários + 50 integração, `mvnw verify` verde.

### 2026-11-11 — Inventário de ativos
- Usuário commitado (`575bb82`) e enviado; PR `feature/backend-usuario` → `main` aberto pelo Lucas. Inventário em `feature/backend-inventario`, criada em cima dela (merge do PR do usuário com "merge commit", não squash).
- Protótipo aprovado (artifact "Conformia Inventário — protótipo"), sem coluna C-I-D na lista (só criticidade, notas no tooltip).
- `V2__cria_ativo.sql`: tabela `ativo` + sequência `ativo_codigo_seq` (código `ATV-001`...), FKs para `usuario` (proprietário, criado_por, atualizado_por), CHECKs de tipo/classificação/notas/fornecedor/justificativa.
- Regras (`AtivoService`): Gerente e Analista alteram, Auditor só lê; proprietário = Gerente/Analista **ativo** (Auditor nunca); terceirizado exige fornecedor (proprietário continua interno, A.5.23); nada é apagado → **descontinuar** com justificativa (10–500); descontinuado não é editável; criticidade = maior nota C-I-D.
- Proprietário desativado: o ativo mantém o dono, a lista mostra "Proprietário inativo" e a edição obriga escolher outro.
- Web `/inventario` (lista + drawer + modal de descontinuar) e API `/api/ativos`. "/" agora abre o inventário para todos os perfis.
- Contrato para o Flutter em `docs/api/inventario.md`.
- Testes: + `AtivoServiceTest` (16), `AtivoTest` (4), `InventarioWebIT` (9), `AtivoApiIT` (8). Total: 47 unitários + 41 integração, `mvnw verify` verde.
- Aprendizados técnicos: Thymeleaf 3.1 proíbe `${param...}` em `th:attr` (montar no controller); Jackson 3 recusa JSON sem campo `boolean` primitivo (usar `Boolean` + construtor compacto do record).

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
| 2026-11-11 | Gerente edita nome/e-mail e ativa/desativa usuários; não pode se desativar; desativado perde a sessão na hora | Pedido do Lucas após o E2E; evita sistema sem gerente e acesso "fantasma" |
| 2026-11-11 | Inventário antes do risco; risco escolhe o ativo do inventário | Risco sempre ligado a um ativo real, sem texto solto |
| 2026-11-11 | Matriz de risco 5×5 (níveis: Baixo 1–4, Médio 5–9, Alto 10–16, Crítico 17–25) | Mais usada em ISO 27001; mesma escala 1–5 da criticidade C-I-D |
| 2026-11-11 | Riscos e ativos: Gerente e Analista alteram, Auditor só lê, nada é excluído (encerrar/descontinuar com justificativa) | Histórico preservado para auditoria |
| 2026-11-11 | Proprietário do ativo = usuário Gerente/Analista ativo; ativo pode ser terceirizado (fornecedor), mas o dono é interno | Responsabilidade não sai da empresa (A.5.9, A.5.23) |
| 2026-11-11 | Classificação em 4 níveis + C-I-D de 1 a 5 por ativo | Esquema mais comum; criticidade sugere o impacto do risco |
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
- [x] Editar usuário (nome, e-mail, ativar/desativar).
- [ ] Decidir se o Gerente pode **mudar o perfil** de um usuário (hoje não pode).
- [x] `POST /api/usuarios` sem o campo `ativo` (corrigido).
- [ ] Merge do PR `feature/backend-usuario` → `main` (usar "Create a merge commit").
