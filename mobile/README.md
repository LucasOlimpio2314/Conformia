# Conformia — app mobile (Flutter)

App Android para o dia a dia: **minhas ações** do plano, **riscos** (consulta),
**aceite formal** (Gerente), **avisos** com notificação e **conta**.
Cadastros (riscos, ativos, usuários, criar ações) ficam na web.

Consome a API `/api/**` do `server/` (contrato em `docs/api/`).

## Rodar

1. Suba o servidor (`server/`, porta 8080).
2. Ligue o emulador **Conformia_Leve** (Android 34 sem apps do Google, 720×1280).
3. Na pasta `mobile/`:

```bash
flutter pub get
flutter run
```

No emulador o servidor é `http://10.0.2.2:8080` (o "localhost" do PC).
Celular de verdade na mesma rede Wi-Fi:

```bash
flutter run --dart-define=API_URL=http://<IP-do-PC>:8080
```

## Testes

```bash
flutter test      # unitários (padrão AAA, como no server)
flutter analyze   # análise estática
```

## Organização (por módulo, como o server)

| Pasta | O que tem |
|---|---|
| `lib/core/` | cliente HTTP (sessão por cookie), tema, componentes, providers globais |
| `lib/auth/` | login, troca obrigatória de senha, sessão (`authProvider`) |
| `lib/acoes/` | minhas ações e atualização (situação + evidência) |
| `lib/riscos/` | lista e detalhe dos riscos, com o plano |
| `lib/aceites/` | pendências e aceite formal (só Gerente) |
| `lib/avisos/` | avisos do sininho |
| `lib/notificacoes/` | notificação local + consulta periódica |
| `lib/conta/` | dados do usuário, alterar senha, sair |

## Como funcionam as notificações (sem Firebase)

- App aberto: consulta `/api/avisos` a cada **1 minuto**.
- App fechado: tarefa do WorkManager a cada **15 minutos** (mínimo permitido pelo Android).
- O maior `id` já notificado fica salvo no celular, então o mesmo aviso nunca é notificado duas vezes.
  Ler o aviso no app tira a notificação da barra.
- Só o perfil Gerente recebe avisos.

## Pacotes

| Pacote | Para quê |
|---|---|
| `flutter_riverpod` | estado (sessão, listas) |
| `dio` + `cookie_jar` + `dio_cookie_manager` | HTTP com o cookie `JSESSIONID` salvo em arquivo |
| `path_provider` | pasta onde o cookie é salvo |
| `flutter_local_notifications` | notificação no Android |
| `workmanager` | consulta em segundo plano |
| `shared_preferences` | último aviso notificado |
