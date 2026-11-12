# Memória do projeto

> Onde o projeto parou. Atualizar ao final de cada etapa (mais recente em cima).

## Estado atual

| | |
|---|---|
| Fase | Plano de ação, aceite formal e avisos implementados na branch `feature/backend-aceite-plano`, aguardando commit do Lucas |
| Próximo passo | Commitar (commit-modulo-aceite-plano.bat) e juntar os PRs na ordem: usuário → inventário → risco → aceite-plano. Depois: definir com o grupo (ex.: relatórios para auditoria, app Flutter) |

## Histórico

### 2026-11-11 — Plano de ação, aceite formal e avisos
- Branch `feature/backend-aceite-plano` (em cima da `feature/backend-risco`, commit `f7bf9ba`). Os dois módulos juntos porque o aceite do residual depende do plano.
- Protótipo aprovado (artifact "Conformia Aceite e Plano — protótipo"). Lucas: controles do Anexo A são lista fixa da ISO, sem tela (item removido do menu).
- `V4__cria_plano_aceite_aviso.sql`: residual esperado no `risco`; `acao_plano` (ACT-001, evidência obrigatória ao concluir); `aceite_risco` (foto do nível aceito, índice único parcial: 1 VIGENTE por risco); `aviso`.
- **Plano** (`PlanoAcaoService`): riscos Mitigar/Transferir/Evitar; ações com responsável Gerente/Analista ativo, prazo, controle do próprio risco, situação e evidência; atrasada = em aberto com prazo vencido; residual ≤ inerente. Plano concluído (todas as ações não canceladas concluídas + residual definido) → aviso RESIDUAL_PRONTO aos Gerentes.
- **Aceite** (`AceiteService`): só Gerente; tipo RISCO (tratamento Aceitar) ou RESIDUAL (plano concluído); justificativa ≥20 + declaração obrigatória; validade 12 meses; revogar com motivo. **Cai automaticamente** (INVALIDADO) quando muda probabilidade/impacto/ativos/tratamento do risco, o residual ou entra ação nova → aviso ACEITE_INVALIDADO com o que mudou e quem mudou. Encerrar o risco derruba sem aviso.
- **Vencimento**: `VencimentoAceiteJob` (@Scheduled 6h) + verificação ao abrir a tela → VENCIDO + aviso ACEITE_VENCIDO.
- **Avisos**: dentro do sistema, um por Gerente ativo; tela `/avisos` (Ver marca como lido e leva à tela); contadores no menu via bean `@menu` chamado do template (`MenuContadores`).
- Lista de riscos: coluna Tratamento mostra "aceito até…", "aguardando aceite" ou "n/m ações".
- `RespostaFetch` (shared/web): padrão X-Redirect/422 usado nos controllers novos.
- **E2E no Chrome** (fluxo completo: criar risco Mitigar → ação atrasada → residual → concluir → aviso → aceite → mudar residual → aceite cai → aviso → Ver → aceitar → revogar). Bugs achados e corrigidos: (1) código ACT era gerado antes da validação e tentativa recusada "queimava" um número; (2) drawer focava o 1º `input` (no aceite era o checkbox da declaração) — agora foca o 1º campo de digitação.
- Testes: + `AceiteServiceTest` (10), `PlanoAcaoServiceTest` (13), `AvisoServiceTest` (3), `RiscoServiceTest` +3, `AceitePlanoWebIT` (12), `AceitePlanoApiIT` (7). Total: 109 unitários + 85 integração.
- Dados de teste no banco de desenvolvimento: RSK-002 (Mitigar, ACT-002 concluída, residual 2×4, aceite revogado).

### 2026-11-11 — Registro de riscos
- Branch `feature/backend-risco` (criada em cima da `feature/backend-inventario`, commit `834462b`).
- Protótipo aprovado após ajustes do Lucas: sem mapa de calor (só 4 cards por nível, clicáveis), sem coluna de situação (risco é ATIVO ou ENCERRADO), coluna "Controles (Anexo A)", ameaça e vulnerabilidade uma embaixo da outra.
- `V3__cria_risco.sql`: catálogos `ameaca` (13 itens) e `vulnerabilidade` (12) com índice único em `lower(nome)`; `controle_anexo_a` com os 93 controles da ISO 27001:2022; `risco` + `risco_ativo` + `risco_controle`; sequência `risco_codigo_seq` (RSK-001).
- Pacote `controle/` criado só com entidade + repository do Anexo A (lista fixa da norma, sem tela).
- Regras (`RiscoService`): ≥1 ativo em uso (na edição pode manter ativo já ligado que foi descontinuado → aviso "Ativo descontinuado"), ≥1 controle, dono Gerente/Analista ativo (aviso "Dono inativo"), probabilidade/impacto 1–5, tratamento opcional ("a definir"), encerrar com justificativa 10–500, encerrado não edita. Catálogos ampliados por Gerente/Analista pelo "+ Nova" (`CatalogoRiscoService`).
- Sugestão de impacto = maior criticidade C-I-D entre os ativos (só sugere, botão "Usar sugestão"); dono sugerido = proprietário do 1º ativo.
- Modal de justificativa virou genérico (`data-justificar-*`): usado no "descontinuar" do inventário e no "encerrar" do risco.
- **Bug achado no E2E**: formulário do risco dava 413 — `FormData` vai como multipart e o Tomcat 11 limita o nº de partes; o `th:field` gera um `_controles` por checkbox (~100 partes). Corrigido no `postar()` do `app.js` enviando `URLSearchParams` (x-www-form-urlencoded). MockMvc não pega isso (não passa pelo Tomcat).
- Testes: + `RiscoServiceTest` (13), `CatalogoRiscoServiceTest` (4), `NivelRiscoTest` (9), `RiscoWebIT` (9), `RiscoApiIT` (7). Total: 80 unitários + 66 integração. E2E no Chrome: criar (com "+ Nova" ameaça, busca de controle, sugestão de impacto), editar, filtros, encerrar.
- Dado de teste no banco de desenvolvimento: RSK-001 (encerrado), ameaça "Exposição pública de backup".

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
| 2026-11-11 | Aceite formal: só Gerente; tipos RISCO e RESIDUAL; 12 meses; cai automaticamente se o risco/plano mudar e Gerentes recebem aviso | ISO 6.1.3 f; o Gerente aceitou um nível específico |
| 2026-11-11 | Plano: ações por risco (responsável, prazo, controle do risco, evidência ao concluir) + residual esperado ≤ inerente | Plano concluído leva o residual para aceite |
| 2026-11-11 | Avisos dentro do sistema (sem e-mail por enquanto), verificação diária de vencimento | Sem dependência nova; pronto para virar e-mail depois |
| 2026-11-11 | Controles do Anexo A = lista fixa da ISO (V3), só leitura, **sem tela própria** (item removido do menu) | É a norma, não um cadastro do usuário |
| 2026-11-11 | Risco: 1+ ativos, 1+ controles do Anexo A, catálogos de ameaça/vulnerabilidade ampliáveis por Gerente/Analista, só ATIVO/ENCERRADO | Pedido do Lucas no protótipo; controles amarram o risco à norma |
| 2026-11-11 | Escalas: probabilidade Rara→Quase certa; impacto Insignificante→Catastrófico | Nomes mais usados em avaliações ISO 27005 |
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
