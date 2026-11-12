# API — registro de riscos (mobile)

Mesmas regras de sessão e formato de erro de [`usuarios.md`](usuarios.md).

## Regras

- **Todos os perfis leem.** Cadastrar, editar, encerrar e ampliar catálogos: só `GERENTE` e `ANALISTA`.
- Risco inerente: `valor = probabilidade × impacto` (1–25) → `nivel`: `BAIXO` 1–4, `MEDIO` 5–9, `ALTO` 10–16, `CRITICO` 17–25.
- Pelo menos **um ativo** (em uso) e **um controle do Anexo A** por risco.
- Dono do risco: Gerente ou Analista **ativo**.
- `tratamento`: `MITIGAR`, `ACEITAR`, `TRANSFERIR`, `EVITAR` ou `null` (a definir).
- Situação: `ATIVO` ou `ENCERRADO` (encerrar exige justificativa de 10 a 500 caracteres; encerrado não é editado).
- Avisos na resposta: ativo do risco descontinuado (`ativos[].emUso: false`) e dono inativo (`donoAtivo: false`).
- **Sugestão de impacto** (só interface): maior `criticidade` entre os ativos escolhidos.

## Endpoints

| Método | Rota | Quem | Body | Sucesso |
|---|---|---|---|---|
| GET | `/api/riscos` | todos | — | 200 `[RiscoResumo]` (maior nível primeiro) |
| GET | `/api/riscos/{id}` | todos | — | 200 `RiscoResumo` |
| GET | `/api/riscos/opcoes?riscoId=` | todos | — | 200 `OpcoesRisco` (tudo para montar o formulário) |
| POST | `/api/riscos` | GERENTE, ANALISTA | `RiscoForm` | 201 `RiscoResumo` |
| PUT | `/api/riscos/{id}` | GERENTE, ANALISTA | `RiscoForm` | 200 `RiscoResumo` |
| POST | `/api/riscos/{id}/encerrar` | GERENTE, ANALISTA | `{"justificativa"}` | 200 `RiscoResumo` |
| POST | `/api/riscos/catalogo/ameacas` | GERENTE, ANALISTA | `{"nome"}` | 201 `{"id","nome"}` |
| POST | `/api/riscos/catalogo/vulnerabilidades` | GERENTE, ANALISTA | `{"nome"}` | 201 `{"id","nome"}` |

## Formatos

```json
// RiscoForm (entrada)
{ "titulo": "Sequestro de dados do ERP", "cenario": null,
  "ativoIds": [3, 5], "ameacaId": 1, "vulnerabilidadeId": 4, "controles": ["8.7", "8.13"],
  "donoId": 2, "probabilidade": 3, "impacto": 5, "tratamento": "MITIGAR" }

// RiscoResumo (saída, resumido)
{ "id": 1, "codigo": "RSK-001", "titulo": "...", "cenario": null,
  "ameaca": {"id": 1, "nome": "Ransomware / malware"}, "vulnerabilidade": {"id": 4, "nome": "..."},
  "ativos": [{"id": 3, "codigo": "ATV-003", "nome": "ERP", "emUso": true, "criticidade": 5}],
  "controles": [{"codigo": "8.13", "nome": "Backup de informações", "tema": 8}],
  "donoId": 2, "donoNome": "Mariana Costa", "donoAtivo": true,
  "probabilidade": 3, "impacto": 5, "valor": 15, "nivel": "ALTO", "tratamento": "MITIGAR",
  "situacao": "ATIVO", "justificativaEncerramento": null, "encerradoEm": null,
  "atualizadoEm": "2026-11-11T12:00:00Z", "atualizadoPorNome": "Rafael Nunes" }

// OpcoesRisco
{ "ativos": [{"id","codigo","nome","criticidade","proprietarioId","emUso"}],
  "ameacas": [{"id","nome"}], "vulnerabilidades": [{"id","nome"}],
  "controles": [{"codigo","nome","tema"}],          // 93 controles, ordem da norma
  "donos": [{"id","nome","perfil"}],
  "probabilidades": [{"valor": 1, "rotulo": "Rara"}, ...],
  "impactos": [{"valor": 1, "rotulo": "Insignificante"}, ...],
  "tratamentos": ["MITIGAR","ACEITAR","TRANSFERIR","EVITAR"] }
```

## Erros comuns

| Status | Quando |
|---|---|
| 400 `DADOS_INVALIDOS` | Falta título, ativos, ameaça, vulnerabilidade, controles, dono ou notas (`campos` diz quais) |
| 403 `ACESSO_NEGADO` | Auditor tentando alterar |
| 422 `REGRA_NEGOCIO` | `campo`: `ativoIds` (descontinuado), `controles` (inexistente), `donoId` (auditor/inativo), `justificativa`, `nome` (item de catálogo repetido) — ou risco encerrado |
