import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/tema.dart';
import '../core/widgets.dart';
import 'risco.dart';
import 'risco_page.dart';
import 'riscos_providers.dart';

/// Consulta dos riscos ativos (todos os perfis). Cadastro e edição ficam na web.
class RiscosPage extends ConsumerStatefulWidget {
  const RiscosPage({super.key});

  @override
  ConsumerState<RiscosPage> createState() => _RiscosPageState();
}

class _RiscosPageState extends ConsumerState<RiscosPage> {
  String _busca = '';
  String? _nivel;

  static const _niveis = {'CRITICO': 'Crítico', 'ALTO': 'Alto', 'MEDIO': 'Médio', 'BAIXO': 'Baixo'};

  @override
  Widget build(BuildContext context) {
    final riscos = ref.watch(riscosProvider);
    return riscos.when(
      skipLoadingOnRefresh: true,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => EstadoErro(e, aoTentar: () => ref.invalidate(riscosProvider)),
      data: (todos) {
        final ativos = todos.where((r) => r.ativo).toList();
        final visiveis = ativos.where((r) => (_nivel == null || r.nivel == _nivel) && r.combina(_busca)).toList();
        return RefreshIndicator(
          onRefresh: () => ref.refresh(riscosProvider.future),
          child: ListView(
            padding: const EdgeInsets.all(12),
            children: [
              TextField(
                key: const Key('riscos-busca'),
                onChanged: (v) => setState(() => _busca = v),
                decoration: const InputDecoration(
                  hintText: 'Buscar por código, ativo ou ameaça',
                  prefixIcon: Icon(Icons.search),
                  isDense: true,
                ),
              ),
              const SizedBox(height: 10),
              Filtros(
                opcoes: [
                  ('TODOS', 'Todos', ativos.length),
                  for (final n in _niveis.entries) (n.key, n.value, ativos.where((r) => r.nivel == n.key).length),
                ],
                selecionada: _nivel ?? 'TODOS',
                aoSelecionar: (id) => setState(() => _nivel = id == 'TODOS' || id == _nivel ? null : id),
              ),
              const SizedBox(height: 10),
              if (visiveis.isEmpty)
                const Padding(
                  padding: EdgeInsets.only(top: 60),
                  child: Vazio('Nenhum risco encontrado.', icone: Icons.search_off),
                ),
              for (final r in visiveis)
                Padding(padding: const EdgeInsets.only(bottom: 8), child: _CartaoRisco(r)),
            ],
          ),
        );
      },
    );
  }
}

class _CartaoRisco extends StatelessWidget {
  const _CartaoRisco(this.risco);

  final Risco risco;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => RiscoPage(risco))),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 5, children: [
            Row(children: [Codigo(risco.codigo), const Spacer(), SeloNivel(risco.nivel, valor: risco.valor)]),
            Text(risco.titulo, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, height: 1.3)),
            Text(
              '${risco.tratamentoRotulo} · ${risco.ativos.map((a) => a.nome).join(', ')}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Cores.ink3, fontSize: 12.5),
            ),
          ]),
        ),
      ),
    );
  }
}
