import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../auth/auth_controller.dart';
import '../core/formatos.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'acao_page.dart';
import 'acoes_providers.dart';
import 'plano.dart';

enum _Filtro { abertas, atrasadas, concluidas }

/// Tela inicial do Gerente e do Analista: as ações do plano em que ele é o responsável.
class MinhasAcoesPage extends ConsumerStatefulWidget {
  const MinhasAcoesPage({super.key});

  @override
  ConsumerState<MinhasAcoesPage> createState() => _MinhasAcoesPageState();
}

class _MinhasAcoesPageState extends ConsumerState<MinhasAcoesPage> {
  _Filtro _filtro = _Filtro.abertas;

  @override
  Widget build(BuildContext context) {
    final usuario = ref.watch(authProvider).value!.usuario;
    final planos = ref.watch(planosProvider);

    return planos.when(
      skipLoadingOnRefresh: true,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => EstadoErro(e, aoTentar: () => ref.invalidate(planosProvider)),
      data: (planos) {
        final todas = minhasAcoes(planos, usuario.id);
        final abertas = todas.where((m) => m.acao.aberta).toList();
        final atrasadas = abertas.where((m) => m.acao.atrasada).toList();
        final concluidas = todas.where((m) => m.acao.situacao == 'CONCLUIDA').toList();
        final visiveis = switch (_filtro) {
          _Filtro.abertas => abertas,
          _Filtro.atrasadas => atrasadas,
          _Filtro.concluidas => concluidas.reversed.toList(),
        };

        return RefreshIndicator(
          onRefresh: () => ref.refresh(planosProvider.future),
          child: ListView(
            padding: const EdgeInsets.all(12),
            children: [
              Filtros(
                opcoes: [
                  (_Filtro.abertas.name, 'Abertas', abertas.length),
                  (_Filtro.atrasadas.name, 'Atrasadas', atrasadas.length),
                  (_Filtro.concluidas.name, 'Concluídas', concluidas.length),
                ],
                selecionada: _filtro.name,
                aoSelecionar: (id) => setState(() => _filtro = _Filtro.values.byName(id)),
              ),
              const SizedBox(height: 10),
              if (visiveis.isEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 60),
                  child: Vazio(switch (_filtro) {
                    _Filtro.abertas => 'Nenhuma ação aberta com você.\nPuxe para baixo para atualizar.',
                    _Filtro.atrasadas => 'Nenhuma ação atrasada.',
                    _Filtro.concluidas => 'Você ainda não concluiu ações.',
                  }, icone: Icons.task_alt),
                ),
              for (final m in visiveis)
                Padding(padding: const EdgeInsets.only(bottom: 8), child: CartaoAcao(m)),
            ],
          ),
        );
      },
    );
  }
}

class CartaoAcao extends StatelessWidget {
  const CartaoAcao(this.item, {super.key, this.dentroDoRisco = false});

  final MinhaAcao item;

  /// No detalhe do risco o código do risco é repetitivo: mostra o responsável.
  final bool dentroDoRisco;

  @override
  Widget build(BuildContext context) {
    final a = item.acao;
    final hoje = DateTime.now();
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: const BorderSide(color: Cores.linha),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => AcaoPage(item))),
        child: Container(
          // Faixa vermelha à esquerda nas atrasadas, como na web.
          decoration: a.atrasada
              ? const BoxDecoration(border: Border(left: BorderSide(color: Cores.erro, width: 3)))
              : null,
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 5, children: [
            Row(children: [Codigo(a.codigo), const Spacer(), SeloSituacao(a, hoje: hoje)]),
            Text(a.titulo, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, height: 1.3)),
            Row(spacing: 8, children: [
              SeloControle(a.controle.codigo),
              Expanded(
                child: Text(
                  dentroDoRisco
                      ? '${a.responsavelNome} · prazo ${dataBr(a.prazo)}'
                      : a.aberta
                          ? '${item.plano.codigo} · prazo ${diaMes(a.prazo)}'
                          : '${item.plano.codigo} · ${item.plano.titulo}',
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: Cores.ink3, fontSize: 12.5),
                ),
              ),
            ]),
          ]),
        ),
      ),
    );
  }
}

/// Situação da ação; atrasada ganha destaque com os dias de atraso.
class SeloSituacao extends StatelessWidget {
  const SeloSituacao(this.acao, {super.key, required this.hoje});

  final Acao acao;
  final DateTime hoje;

  @override
  Widget build(BuildContext context) {
    if (acao.atrasada) {
      final dias = acao.diasDeAtraso(hoje);
      return Etiqueta('Atrasada · $dias ${dias == 1 ? 'dia' : 'dias'}', fundo: Cores.erroFundo, cor: Cores.erro);
    }
    final rotulo = situacoesAcao[acao.situacao] ?? acao.situacao;
    return switch (acao.situacao) {
      'EM_ANDAMENTO' => Etiqueta(rotulo, fundo: Cores.indigo50, cor: Cores.indigo),
      'CONCLUIDA' => Etiqueta(rotulo, fundo: Cores.okFundo, cor: Cores.ok),
      _ => Etiqueta(rotulo, fundo: Cores.linha2, cor: Cores.ink2),
    };
  }
}
