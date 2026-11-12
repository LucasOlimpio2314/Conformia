import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/tema.dart';
import '../core/widgets.dart';
import 'aceite.dart';
import 'aceite_page.dart';
import 'aceites_providers.dart';

/// Aba do Gerente: riscos que precisam do aceite formal.
class AceitesPage extends ConsumerWidget {
  const AceitesPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pendencias = ref.watch(pendenciasProvider);
    return pendencias.when(
      skipLoadingOnRefresh: true,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => EstadoErro(e, aoTentar: () => ref.invalidate(pendenciasProvider)),
      data: (lista) => RefreshIndicator(
        onRefresh: () => ref.refresh(pendenciasProvider.future),
        child: ListView(
          padding: const EdgeInsets.all(12),
          children: [
            if (lista.isEmpty)
              const Padding(
                padding: EdgeInsets.only(top: 80),
                child: Vazio('Nenhum risco aguardando aceite.', icone: Icons.verified_outlined),
              ),
            for (final p in lista)
              Padding(padding: const EdgeInsets.only(bottom: 8), child: CartaoPendencia(p)),
          ],
        ),
      ),
    );
  }
}

class CartaoPendencia extends StatelessWidget {
  const CartaoPendencia(this.pendencia, {super.key, this.clicavel = true});

  final PendenciaAceite pendencia;
  final bool clicavel;

  @override
  Widget build(BuildContext context) {
    final p = pendencia;
    final conteudo = Padding(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 5, children: [
        Row(children: [
          Codigo('${p.risco.codigo} · ${p.residual ? 'residual' : 'risco'}'),
          const Spacer(),
          SeloNivel(p.nivel, valor: p.valor),
        ]),
        Text(p.risco.titulo, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, height: 1.3)),
        Text(
          p.residual ? 'Plano concluído · ${p.acoesConcluidas} ações' : 'Tratamento: Aceitar',
          style: const TextStyle(color: Cores.ink3, fontSize: 12.5),
        ),
      ]),
    );
    return Card(
      clipBehavior: Clip.antiAlias,
      child: clicavel
          ? InkWell(
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => AceitePage(p))),
              child: conteudo,
            )
          : conteudo,
    );
  }
}
