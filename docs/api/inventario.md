# API — inventário de ativos (mobile)

Mesmas regras de sessão e formato de erro de [`usuarios.md`](usuarios.md).

## Regras

- **Todos os perfis leem.** Cadastrar, editar e descontinuar: só `GERENTE` e `ANALISTA` (Auditor recebe `403 ACESSO_NEGADO`).
- **Proprietário**: um Gerente ou Analista **ativo** — use `GET /api/ativos/proprietarios` para montar a lista.
- **Terceirizado** (`terceirizado: true`) exige `fornecedor`. O proprietário continua interno.
- **Nada é apagado**: ativo sai de uso por `descontinuar` (com justificativa de 10 a 500 caracteres) e não pode mais ser editado.
- `criticidade` = maior nota entre C, I e D (calculada pelo servidor).
- `proprietarioAtivo: false` em ativo `EM_USO` → mostrar aviso "Proprietário inativo".

## Endpoints

| Método | Rota | Quem | Body | Sucesso |
|---|---|---|---|---|
| GET | `/api/ativos` | todos | — | 200 `[AtivoResumo]` |
| GET | `/api/ativos/{id}` | todos | — | 200 `AtivoResumo` |
| GET | `/api/ativos/proprietarios` | todos | — | 200 `[{"id","nome","perfil"}]` |
| POST | `/api/ativos` | GERENTE, ANALISTA | `AtivoForm` | 201 `AtivoResumo` (com `codigo` gerado) |
| PUT | `/api/ativos/{id}` | GERENTE, ANALISTA | `AtivoForm` | 200 `AtivoResumo` |
| POST | `/api/ativos/{id}/descontinuar` | GERENTE, ANALISTA | `{"justificativa"}` | 200 `AtivoResumo` |

## Formatos

```json
// AtivoForm (entrada). descricao, localizacao e fornecedor são opcionais; terceirizado omitido = false
{ "nome": "ERP Financeiro", "tipo": "SOFTWARE", "descricao": "...", "localizacao": "sa-east-1",
  "proprietarioId": 2, "terceirizado": true, "fornecedor": "AWS", "classificacao": "CONFIDENCIAL",
  "confidencialidade": 4, "integridade": 5, "disponibilidade": 4 }

// AtivoResumo (saída)
{ "id": 9, "codigo": "ATV-009", "nome": "ERP Financeiro", "tipo": "SOFTWARE", "descricao": null,
  "localizacao": "sa-east-1", "proprietarioId": 2, "proprietarioNome": "Mariana Costa", "proprietarioAtivo": true,
  "terceirizado": true, "fornecedor": "AWS", "classificacao": "CONFIDENCIAL",
  "confidencialidade": 4, "integridade": 5, "disponibilidade": 4, "criticidade": "MUITO_ALTA",
  "situacao": "EM_USO", "justificativaDescontinuacao": null, "descontinuadoEm": null,
  "atualizadoEm": "2026-11-11T12:00:00Z", "atualizadoPorNome": "Rafael Nunes" }
```

| Campo | Valores |
|---|---|
| `tipo` | `INFORMACAO`, `PROCESSO`, `SOFTWARE`, `HARDWARE`, `REDE`, `SERVICO_TERCEIRO`, `PESSOAS`, `INSTALACAO` |
| `classificacao` | `PUBLICA`, `INTERNA`, `CONFIDENCIAL`, `RESTRITA` |
| C, I, D | 1 a 5 (1 muito baixo … 5 muito alto) |
| `criticidade` | `MUITO_BAIXA`, `BAIXA`, `MEDIA`, `ALTA`, `MUITO_ALTA` |
| `situacao` | `EM_USO`, `DESCONTINUADO` |

## Erros comuns

| Status | Quando |
|---|---|
| 400 `DADOS_INVALIDOS` | Falta nome, tipo, proprietário, classificação ou alguma nota C-I-D (`campos` diz quais) |
| 403 `ACESSO_NEGADO` | Auditor tentando alterar |
| 422 `REGRA_NEGOCIO` | `campo`: `proprietarioId` (auditor/inativo), `fornecedor` (terceirizado sem fornecedor), `justificativa` (curta) — ou ativo descontinuado |
