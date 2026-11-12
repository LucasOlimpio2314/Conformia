import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/providers.dart';
import '../notificacoes/agendador_avisos.dart';
import 'usuario.dart';

/// Quem está logado. `null` = ninguém (mostra o login).
///
/// O app decide a tela inicial só olhando este provider:
/// carregando → splash, null → login, troca obrigatória → troca de senha, senão → abas.
final authProvider = AsyncNotifierProvider<AuthController, Sessao?>(AuthController.new);

class AuthController extends AsyncNotifier<Sessao?> {
  ApiClient get _api => ref.read(apiProvider);

  @override
  Future<Sessao?> build() async {
    // Qualquer 401 em qualquer tela (sessão expirou, usuário desativado) volta ao login.
    _api.aoPerderSessao = (_) {
      if (state.value != null) {
        AgendadorAvisos.parar();
        state = const AsyncData(null);
      }
    };
    try {
      // Ao abrir o app: o cookie salvo ainda vale?
      final sessao = Sessao.deJson(await _api.get('/api/auth/me') as Map<String, dynamic>);
      await _aoEntrar(sessao);
      return sessao;
    } on ApiException catch (e) {
      if (e.status == 401) return null;
      rethrow; // sem conexão: a tela mostra "Tentar de novo"
    }
  }

  /// Lança [ApiException] com a mensagem pronta (ex.: e-mail ou senha incorretos).
  Future<void> entrar(String email, String senha) async {
    final json = await _api.post('/api/auth/login', {'email': email.trim(), 'senha': senha});
    final sessao = Sessao.deJson(json as Map<String, dynamic>);
    await _aoEntrar(sessao);
    state = AsyncData(sessao);
  }

  Future<void> trocarSenhaObrigatoria(String novaSenha, String confirmacao) async {
    await _api.post('/api/auth/trocar-senha', {'novaSenha': novaSenha, 'confirmacao': confirmacao});
    final sessao = Sessao.deJson(await _api.get('/api/auth/me') as Map<String, dynamic>);
    await _aoEntrar(sessao);
    state = AsyncData(sessao);
  }

  Future<void> sair() async {
    try {
      await _api.post('/api/auth/logout');
    } on ApiException {
      // Mesmo sem servidor, o app esquece a sessão.
    }
    await _api.limparSessao();
    await AgendadorAvisos.parar();
    ref.read(abaProvider.notifier).abrir('acoes');
    state = const AsyncData(null);
  }

  /// Depois do login: só o Gerente recebe avisos, então só ele tem a consulta em segundo plano.
  Future<void> _aoEntrar(Sessao sessao) async {
    if (sessao.trocaSenhaObrigatoria) return;
    final usuario = sessao.usuario;
    if (usuario.gerente) {
      await AgendadorAvisos.iniciar(usuario.id);
    } else {
      await AgendadorAvisos.parar();
    }
    // A aba inicial não é escolhida aqui: um provider não pode alterar outro durante o build.
    // A barra de abas cai na primeira aba válida do perfil (Auditor → Riscos).
  }
}
