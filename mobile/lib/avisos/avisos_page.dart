import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';
import '../core/formatos.dart';
import '../core/providers.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'aviso_resumo.dart';
import 'avisos_providers.dart';

/// Avisos do sininho. Tocar num aviso marca como lido e abre a aba Aceites
/// (hoje todo aviso aponta para `/aceites`).
class AvisosPage extends ConsumerWidget {
  const AvisosPage({super.key});

  Future<void> _abrir(BuildContext context, WidgetRef ref, AvisoResumo aviso) async {
    try {
      await marcarLido(ref, aviso);
    } on ApiException catch (e) {
      if (context.mounted) mostrarMensagem(context, e.mensagem);
      return;
    }
    if (aviso.link == '/aceites') {
      ref.read(abaProvider.notifier).abrir('aceites');
      if (context.mounted) Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final avisos = ref.watch(avisosProvider);
    final temNaoLido = avisos.value?.any((a) => !a.lido) ?? false;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Avisos'),
        actions: [
          if (temNaoLido)
            TextButton(
              onPressed: () async {
                try {
                  await marcarTodosLidos(ref);
                } on ApiException catch (e) {
                  if (context.mounted) mostrarMensagem(context, e.mensagem);
                }
              },
              child: const Text('Marcar todos como lidos'),
            ),
        ],
      ),
      body: avisos.when(
        skipLoadingOnRefresh: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EstadoErro(e, aoTentar: () => ref.invalidate(avisosProvider)),
        data: (lista) => RefreshIndicator(
          onRefresh: () => ref.refresh(avisosProvider.future),
          child: lista.isEmpty
              ? ListView(children: const [
                  Padding(
                    padding: EdgeInsets.only(top: 100),
                    child: Vazio('Nenhum aviso por enquanto.', icone: Icons.notifications_none),
                  ),
                ])
              : ListView.separated(
                  itemCount: lista.length,
                  separatorBuilder: (_, _) => const Divider(height: 1, color: Cores.linha2),
                  itemBuilder: (context, i) => _ItemAviso(lista[i], aoTocar: () => _abrir(context, ref, lista[i])),
                ),
        ),
      ),
    );
  }
}

class _ItemAviso extends StatelessWidget {
  const _ItemAviso(this.aviso, {required this.aoTocar});

  final AvisoResumo aviso;
  final VoidCallback aoTocar;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: aviso.lido ? Colors.transparent : Cores.superficie,
      child: InkWell(
        onTap: aoTocar,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, spacing: 10, children: [
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: aviso.lido ? Colors.transparent : Cores.indigo,
                  shape: BoxShape.circle,
                ),
              ),
            ),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 3, children: [
                Text(aviso.titulo,
                    style: TextStyle(
                      fontWeight: aviso.lido ? FontWeight.w500 : FontWeight.w600,
                      color: aviso.lido ? Cores.ink2 : Cores.ink,
                    )),
                Text(aviso.mensagem, style: const TextStyle(color: Cores.ink2, fontSize: 13, height: 1.35)),
                Text(quando(aviso.criadoEm),
                    style: const TextStyle(color: Cores.ink3, fontSize: 11.5, fontFamily: 'monospace')),
              ]),
            ),
          ]),
        ),
      ),
    );
  }
}
