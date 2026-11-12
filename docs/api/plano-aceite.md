# API — plano de ação, aceite formal e avisos (mobile)

Mesmas regras de sessão e formato de erro de [`usuarios.md`](usuarios.md).

## Como as peças se encaixam

```
Risco "Aceitar"  ───────────────────────────────────────────►  Aceite (tipo RISCO, nível inerente)
Risco "Mitigar/Transferir/Evitar" → ações + residual esperado
        todas as ações concluídas + residual definido  ─────►  Aceite (tipo RESIDUAL, nível residual)
Aceite vigente: vale 12 meses · cai se o risco/plano mudar (Gerentes avisados) · Gerente pode revogar
```

## Plano de ação

**Todos leem; Gerente e Analista alteram.** Só riscos ativos com tratamento `MITIGAR`, `TRANSFERIR` ou `EVITAR`.

| Método | Rota | Body | Sucesso |
|---|---|---|---|
| GET | `/api/plano` | — | 200 `[PlanoRisco]` (maior nível primeiro) |
| GET | `/api/plano/indicadores` | — | 200 `{"emAberto","atrasadas","concluidas","prontosParaAceite"}` |
| GET | `/api/plano/{riscoId}` | — | 200 `PlanoRisco` |
| GET | `/api/plano/{riscoId}/opcoes` | — | 200 `{"responsaveis","controles","situacoes"}` (controles **do risco**) |
| POST | `/api/plano/{riscoId}/acoes` | `AcaoForm` | 201 `AcaoResumo` (código `ACT-001`) |
| PUT | `/api/plano/acoes/{id}` | `AcaoForm` | 200 `AcaoResumo` |
| PUT | `/api/plano/{riscoId}/residual` | `{"probabilidade","impacto"}` | 200 `PlanoRisco` |

```json
// AcaoForm — prazo em ISO; evidencia obrigatória quando situacao = CONCLUIDA
{ "titulo": "Ativar MFA no CRM", "responsavelId": 2, "prazo": "2026-12-15",
  "controle": "8.5", "situacao": "PENDENTE", "evidencia": null }
```

- `situacao`: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA` (as duas últimas não podem mais ser alteradas).
- `atrasada: true` = em aberto com prazo vencido.
- Residual esperado ≤ inerente. Mudar o residual ou incluir ação depois do aceite derruba o aceite.

## Aceite formal

**Todos leem; só Gerente aceita e revoga.**

| Método | Rota | Body | Sucesso |
|---|---|---|---|
| GET | `/api/aceites/pendencias` | — | 200 `[PendenciaAceite]` (`tipo`, nível a aceitar, `anterior` = por que o último caiu) |
| GET | `/api/aceites` | — | 200 `[AceiteResumo]` (histórico completo) |
| POST | `/api/aceites/riscos/{riscoId}` | `{"justificativa","condicoes","declaracao": true}` | 201 `AceiteResumo` |
| POST | `/api/aceites/{id}/revogar` | `{"motivo"}` | 200 `AceiteResumo` |

- `justificativa`: 20 a 1000 caracteres; `declaracao` precisa ser `true` (o Gerente declara que aceita).
- `situacao`: `VIGENTE`, `VENCIDO` (12 meses), `REVOGADO`, `INVALIDADO` (risco/plano mudou).

## Avisos (sininho do app)

| Método | Rota | Sucesso |
|---|---|---|
| GET | `/api/avisos` | 200 `[{"id","tipo","titulo","mensagem","link","criadoEm","lido"}]` |
| GET | `/api/avisos/nao-lidos` | 200 `{"quantidade": 2}` |
| POST | `/api/avisos/{id}/lido` | 200 `{"link": "/aceites"}` |
| POST | `/api/avisos/lidos` | 204 |

`tipo`: `ACEITE_INVALIDADO`, `ACEITE_VENCIDO`, `RESIDUAL_PRONTO`. Hoje só Gerentes recebem avisos.
