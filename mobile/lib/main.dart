import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:workmanager/workmanager.dart';

import 'auth/auth_controller.dart';
import 'auth/login_page.dart';
import 'auth/trocar_senha_page.dart';
import 'core/api_client.dart';
import 'core/providers.dart';
import 'core/tema.dart';
import 'core/widgets.dart';
import 'inicio_page.dart';
import 'notificacoes/notificador.dart';
import 'notificacoes/tarefa_segundo_plano.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final api = await ApiClient.criar();
  await Notificador.iniciar();
  await Workmanager().initialize(tarefaSegundoPlano);

  runApp(ProviderScope(
    overrides: [apiProvider.overrideWithValue(api)],
    // Riverpod 3 repete sozinho um provider que deu erro. Aqui não queremos:
    // erro de rede mostra "Tentar de novo" e 401 volta para o login.
    retry: (_, _) => null,
    child: const ConformiaApp(),
  ));
}

final _navegador = GlobalKey<NavigatorState>();

class ConformiaApp extends StatelessWidget {
  const ConformiaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Conformia',
      debugShowCheckedModeBanner: false,
      navigatorKey: _navegador,
      theme: temaConformia(),
      home: const _Raiz(),
    );
  }
}

/// Escolhe a tela pela sessão: carregando → splash, sem sessão → login,
/// senha temporária → troca obrigatória, senão → abas.
class _Raiz extends ConsumerWidget {
  const _Raiz();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Perdeu a sessão com uma tela aberta por cima (ex.: detalhe da ação)? Fecha tudo.
    ref.listen(authProvider, (antes, depois) {
      if (antes?.value != null && depois.value == null) {
        _navegador.currentState?.popUntil((r) => r.isFirst);
      }
    });

    final auth = ref.watch(authProvider);
    return auth.when(
      skipLoadingOnReload: true,
      loading: () => const Scaffold(backgroundColor: Cores.indigo, body: Center(child: CircularProgressIndicator(color: Colors.white))),
      error: (e, _) => Scaffold(body: EstadoErro(e, aoTentar: () => ref.invalidate(authProvider))),
      data: (sessao) {
        if (sessao == null) return const LoginPage();
        if (sessao.trocaSenhaObrigatoria) return const TrocarSenhaPage();
        return const InicioPage();
      },
    );
  }
}
