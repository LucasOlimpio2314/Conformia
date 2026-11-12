import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import 'aceites/aceites_page.dart';
import 'aceites/aceites_providers.dart';
import 'acoes/minhas_acoes_page.dart';
import 'auth/auth_controller.dart';
import 'auth/usuario.dart';
import 'avisos/avisos_page.dart';
import 'avisos/avisos_providers.dart';
import 'conta/conta_page.dart';
import 'core/api_client.dart';
import 'core/config.dart';
import 'core/providers.dart';
import 'core/tema.dart';
import 'notificacoes/notificador.dart';
import 'notificacoes/verificador_avisos.dart';
import 'riscos/riscos_page.dart';

typedef _Aba = ({String id, String titulo, IconData icone, Widget pagina});

/// Abas que cada perfil vê (protótipo aprovado):
/// Gerente: Ações, Riscos, Aceites, Conta · Analista: Ações, Riscos, Conta · Auditor: Riscos, Conta.
List<_Aba> _abasDo(Usuario u) => [
      if (u.podeAlterar)
        (id: 'acoes', titulo: 'Minhas ações', icone: Icons.task_alt, pagina: const MinhasAcoesPage()),
      (id: 'riscos', titulo: 'Riscos', icone: Icons.warning_amber_rounded, pagina: const RiscosPage()),
      if (u.gerente)
        (id: 'aceites', titulo: 'Aceites', icone: Icons.verified_outlined, pagina: const AceitesPage()),
      (id: 'conta', titulo: 'Conta', icone: Icons.person_outline, pagina: const ContaPage()),
    ];

class InicioPage extends ConsumerStatefulWidget {
  const InicioPage({super.key});

  @override
  ConsumerState<InicioPage> createState() => _InicioPageState();
}

class _InicioPageState extends ConsumerState<InicioPage> {
  Timer? _timer;
  AppLifecycleListener? _ciclo;

  bool get _gerente => ref.read(authProvider).value?.usuario.gerente ?? false;

  @override
  void initState() {
    super.initState();
    if (!_gerente) return;

    // Tocar na notificação (app aberto ou em segundo plano) leva para Aceites.
    Notificador.aoTocar = _abrirAceites;
    Notificador.abriuPorNotificacao().then((sim) {
      if (sim) _abrirAceites();
    });

    // App aberto: confere avisos a cada 1 minuto e sempre que volta para a tela.
    _timer = Timer.periodic(intervaloAvisosAberto, (_) => _verificarAvisos());
    _ciclo = AppLifecycleListener(onResume: _verificarAvisos);
    _verificarAvisos();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _ciclo?.dispose();
    Notificador.aoTocar = null;
    super.dispose();
  }

  void _abrirAceites() {
    if (!mounted) return;
    Navigator.of(context).popUntil((r) => r.isFirst);
    ref.read(abaProvider.notifier).abrir('aceites');
  }

  Future<void> _verificarAvisos() async {
    try {
      await VerificadorAvisos.verificar(ref.read(apiProvider));
      if (!mounted) return;
      ref.invalidate(naoLidosProvider);
      ref.invalidate(pendenciasProvider);
    } on ApiException {
      // 401 já volta ao login pelo AuthController; sem conexão, tenta no próximo minuto.
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = ref.watch(authProvider).value!.usuario;
    final abas = _abasDo(usuario);
    final pedida = ref.watch(abaProvider);
    // Aba pedida que o perfil não tem (ex.: Auditor e "acoes") cai na primeira.
    final indice = abas.indexWhere((a) => a.id == pedida).clamp(0, abas.length - 1);

    final naoLidos = usuario.gerente ? (ref.watch(naoLidosProvider).value ?? 0) : 0;
    final pendencias = usuario.gerente ? (ref.watch(pendenciasProvider).value?.length ?? 0) : 0;

    return Scaffold(
      appBar: AppBar(
        title: Text(abas[indice].titulo),
        actions: [
          if (usuario.gerente)
            IconButton(
              key: const Key('sininho'),
              tooltip: 'Avisos',
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AvisosPage())),
              icon: Badge(
                isLabelVisible: naoLidos > 0,
                label: Text('$naoLidos'),
                backgroundColor: Cores.erro,
                child: const Icon(Icons.notifications_none),
              ),
            ),
        ],
      ),
      // IndexedStack mantém as abas vivas: trocar de aba não recarrega a lista.
      body: IndexedStack(index: indice, children: [for (final a in abas) a.pagina]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: indice,
        onDestinationSelected: (i) => ref.read(abaProvider.notifier).abrir(abas[i].id),
        destinations: [
          for (final a in abas)
            NavigationDestination(
              label: a.id == 'acoes' ? 'Ações' : a.titulo,
              icon: Badge(
                isLabelVisible: a.id == 'aceites' && pendencias > 0,
                label: Text('$pendencias'),
                backgroundColor: Cores.erro,
                child: Icon(a.icone),
              ),
            ),
        ],
      ),
    );
  }
}
