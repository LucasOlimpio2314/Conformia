# Regras gerais do Conformia

> Regras fixas do projeto. Mudam pouco. O andamento fica em `MEMORIA.md`.

## 1. O projeto

| Item | Valor |
|---|---|
| O que é | Plataforma de gestão de registros ISO 27001 (riscos, aceite formal, plano de ação, inventário, controles do Anexo A) |
| Módulo atual | **riskdesk** (gestão de risco), começando por **usuário** (cadastro + login) |
| Uso acadêmico | Base do Trabalho Integrador da UNINOVE (grupo de 5) — entrega final em **01/12/2026** |
| Perfis | `GERENTE`, `ANALISTA`, `AUDITOR` (auditor = só leitura) |
| Regra de acesso | Sem auto-cadastro público: usuários são criados por um gerente. Um usuário inicial é criado automaticamente para o primeiro acesso |

## 2. Stack

| Camada | Tecnologia |
|---|---|
| Backend + web | Java 21, Spring Boot 4.1.x, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, Validation |
| Banco | PostgreSQL + Flyway (migrations) |
| Mobile | Flutter (pasta `mobile/`, consome `/api/**`) |
| Build | Maven (usar o wrapper: `mvnw`) |

Não adicionar dependência, framework ou trocar tecnologia sem perguntar antes.

## 3. Estrutura do repositório (monorepo)

```
Conformia/
 ├─ server/   → Spring: telas web (Thymeleaf) + API do mobile + regras + banco
 ├─ mobile/   → Flutter (ainda não criado)
 └─ docs/     → documentação (docs/ia = arquivos para IA)
```

Pacote base: `io.github.lucasolimpio.conformia`

Organização **por módulo**, com camadas dentro de cada módulo:

```
<modulo>/
 ├─ domain/      entidades JPA e enums
 ├─ repository/  interfaces Spring Data
 ├─ service/     regra de negócio (ÚNICO lugar com regra)
 ├─ dto/         records de entrada/saída
 ├─ web/         @Controller → Thymeleaf (navegador)
 └─ api/         @RestController em /api/** → JSON (Flutter)
shared/          config, security, exception — usado por todos
```

Criar pastas de um módulo só quando o módulo for implementado.

## 4. Convenções de código

| Regra | Motivo |
|---|---|
| Regra de negócio só em `service/` — controllers só recebem, validam formato e delegam | Web e mobile usam a mesma regra; nada duplicado |
| Entidade JPA nunca sai para tela ou JSON — usar DTO (`record`) | Não expor campos sensíveis (senha) nem causar loop em relacionamentos |
| Sem Lombok | Objetivo é aprender; DTOs como `record` |
| Injeção por construtor (sem `@Autowired` em campo) | Testável e dependências explícitas |
| Erros de negócio → `RegraNegocioException`; tratados em `WebExceptionHandler` (página) e `ApiExceptionHandler` (JSON) | Mesmo erro, resposta adequada a cada cliente |
| Nomes de domínio em português (`Usuario`, `Risco`, `AceiteRisco`) | Coerente com o negócio |

## 5. Banco e migrations

- Toda mudança de schema é uma migration Flyway em `server/src/main/resources/db/migration`.
- Nome: `V<n>__descricao_em_snake_case.sql` (ex.: `V1__cria_usuario.sql`).
- **Nunca editar uma migration já commitada** — criar uma nova.
- `ddl-auto=validate`: o Hibernate não cria tabela.

## 6. Segurança

- Credenciais só no `server/.env` (ignorado pelo Git). Modelo em `.env.example`.
- Nunca colocar senha, token ou string de conexão com senha no código, em commit ou em documentação.
- Senhas de usuário sempre com hash (BCrypt via `PasswordEncoder`).
- Usuário do banco da aplicação próprio (`conformia_app`), nunca superuser.

## 7. Testes

| Tipo | Sufixo | Como | Roda com |
|---|---|---|---|
| Unitário | `*Test` | JUnit 5 + Mockito, **sem subir Spring**, testa `service/` | `mvnw test` |
| Integração | `*IT` | `@SpringBootTest` + `@ActiveProfiles("test")`, banco `conformia_test` | `mvnw verify` |

- Local: `server/src/test/java/...` espelhando o pacote da classe testada.
- Nome do método descreve o comportamento: `deveBloquearAceiteQuandoUsuarioNaoForGerente()`.
- **Padrão AAA** em todo teste, com os comentários marcando as fases:

```java
@Test
void deveImpedirAnalistaDeAceitarRisco() {
    // Arrange — prepara dados e mocks
    // Act     — executa UMA ação
    // Assert  — verifica o resultado
}
```

- Um comportamento por teste (um único Act). Vários cenários = vários métodos `@Test`.
- Asserções com **AssertJ** (`assertThat(...)`), não `assertEquals`.
- **Depois de cada implementação:** criar o teste dela e rodar `mvnw verify` (regressão completa) antes de commitar.

## 8. Git

- Branch `main` protegida; trabalho em `feature/<area>-<descricao>` (ex.: `feature/backend-login`) e integração por PR.
- Commits no padrão: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`.
- Antes de commitar: `git status` e conferir que `.env`, `target/` e `.idea/` não aparecem.

## 9. Interface

- Tema claro, cor principal índigo `#4338CA`.
- Cadastro/edição em drawer lateral à direita, carregado como fragmento Thymeleaf via fetch (`/usuarios/{id}/form`).
- **Antes de implementar ou mudar uma tela, mostrar o design para aprovação.**

## 10. Como a IA deve trabalhar neste projeto

1. Ler `MEMORIA.md` no início para saber onde parou.
2. **Ensinar, não só entregar**: explicar o porquê e como pensar a solução, como um sênior com um júnior.
3. Antes de entregar uma solução pronta, perguntar se o Lucas quer a **resposta completa** ou **só o caminho** para ele implementar.
4. Backend e integração: o Lucas escreve para aprender — a IA guia e revisa. HTML/CSS pode ser gerado pela IA.
5. Perguntar antes de criar arquivos, mudar estrutura, adicionar dependência ou fazer ação irreversível.
6. Não fazer commit nem push — o Lucas faz.
7. Respostas diretas e objetivas; comparações em tabela.
8. **Ao final de cada etapa, atualizar `MEMORIA.md`** (o que foi feito, decisões e próximo passo).
