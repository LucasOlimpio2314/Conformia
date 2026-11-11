# API — autenticação e usuários (mobile)

Base: `http://<servidor>:8080`. Corpo e respostas em JSON (`Content-Type: application/json`).

## Como funciona o login

Por **sessão**, igual à web — sem JWT.

1. `POST /api/auth/login` → o servidor responde com o cookie `JSESSIONID`.
2. O app guarda esse cookie e o envia em **todas** as chamadas seguintes.
3. A sessão expira após **30 min sem uso** → respostas `401 NAO_AUTENTICADO`: voltar para o login.
4. Se o login devolver `"trocaSenhaObrigatoria": true`, abrir a tela de troca. Até trocar, as outras rotas respondem `403 TROCA_SENHA_OBRIGATORIA`.

Não é preciso token CSRF na API.

## Endpoints

| Método | Rota | Quem | Body | Sucesso |
|---|---|---|---|---|
| POST | `/api/auth/login` | todos | `{"email","senha"}` | 200 `SessaoResposta` |
| GET | `/api/auth/me` | logado | — | 200 `SessaoResposta` |
| POST | `/api/auth/trocar-senha` | logado com senha temporária | `{"novaSenha","confirmacao"}` | 204 |
| POST | `/api/auth/logout` | logado | — | 204 |
| GET | `/api/minha-conta` | logado | — | 200 `UsuarioResposta` |
| PUT | `/api/minha-conta/senha` | logado | `{"senhaAtual","novaSenha","confirmacao"}` | 204 |
| GET | `/api/usuarios` | GERENTE | — | 200 `[UsuarioResposta]` |
| POST | `/api/usuarios` | GERENTE | `{"nome","email","perfil","ativo"}` | 201 `SenhaTemporariaGerada` |
| POST | `/api/usuarios/{id}/redefinir-senha` | GERENTE (não para si) | — | 200 `SenhaTemporariaGerada` |

`perfil`: `GERENTE`, `ANALISTA` ou `AUDITOR`.
Regras da nova senha: 8 a 64 caracteres, letras e números, diferente da atual.

## Formatos

```json
// SessaoResposta
{ "usuario": { ...UsuarioResposta }, "trocaSenhaObrigatoria": false }

// UsuarioResposta (datas em ISO-8601 UTC)
{ "id": 1, "nome": "Administrador", "email": "admin@conformia.local", "perfil": "GERENTE",
  "ativo": true, "situacao": "ATIVO", "ultimoAcesso": "2026-11-11T12:00:00Z", "criadoEm": "2026-11-11T11:00:00Z" }
// situacao: ATIVO | AGUARDANDO_PRIMEIRO_ACESSO | SENHA_REDEFINIDA | INATIVO

// SenhaTemporariaGerada — a senha só aparece nesta resposta; mostrar uma vez e descartar
{ "usuarioId": 7, "nomeUsuario": "Juliana Prado", "senha": "Kq7mV2pXw9" }
```

## Erros

Sempre `{"codigo", "mensagem", "campo"?, "campos"?}`. A `mensagem` já vem pronta para exibir.

| Status | codigo | Quando |
|---|---|---|
| 400 | `DADOS_INVALIDOS` | Campo vazio/formato errado — `campos` traz `{campo: mensagem}` |
| 400 | `JSON_INVALIDO` | JSON malformado ou valor inexistente (ex.: perfil) |
| 401 | `CREDENCIAIS_INVALIDAS` | Login: e-mail ou senha incorretos (ou usuário inativo) |
| 401 | `SENHA_EXPIRADA` | Login: senha temporária passou das 24 h |
| 401 | `NAO_AUTENTICADO` | Sem sessão ou sessão expirada |
| 403 | `ACESSO_NEGADO` | Perfil sem permissão (ex.: analista em `/api/usuarios`) |
| 403 | `TROCA_SENHA_OBRIGATORIA` | Precisa trocar a senha temporária antes |
| 422 | `REGRA_NEGOCIO` | Regra violada (ex.: e-mail repetido) — `campo` indica qual |
