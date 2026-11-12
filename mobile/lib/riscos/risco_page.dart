import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../acoes/minhas_acoes_page.dart';
import '../core/formatos.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'risco.dart';
import 'riscos_providers.dart';

/// Detalhe do risco (só leitura): cenário, ativos, ameaça e vulnerabilidade
/// (uma embaixo da outra, como pedido na web), controles e o plano de ação.
class RiscoPage extends ConsumerWidget {
  const RiscoPage(this.risco, {super.key});

  final Risco risco;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: Text(risco.codigo)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Column(crossAxisAlignment: CrossAxisAlignment.stretch, spacing: 14, children: [
            Row(crossAxisAlignment: CrossAxisAlignment.start, spacing: 10, children: [
              Expanded(
                child: Text(risco.titulo,
                    style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600, height: 1.3)),
              ),
              SeloNivel(risco.nivel, valor: risco.valor),
            ]),
            if (risco.cenario != null && risco.cenario!.isNotEmpty)
              Text(risco.cenario!, style: const TextStyle(color: Cores.ink2, height: 1.4)),
            _Bloco('Avaliação', [
              _Campo('Probabilidade', '${risco.probabilidade} · ${escalaProbabilidade[risco.probabilidade]}'),
              _Campo('Impacto', '${risco.impacto} · ${escalaImpacto[risco.impacto]}'),
              _Campo('Tratamento', risco.tratamentoRotulo),
              _Campo('Dono', risco.donoNome),
            ]),
            _Bloco('Cenário de ameaça', [
              _Campo('Ameaça', risco.ameaca, empilhado: true),
              _Campo('Vulnerabilidade', risco.vulnerabilidade, empilhado: true),
            ]),
            _Bloco('Ativos', [
              for (final a in risco.ativos)
                Row(spacing: 8, children: [
                  Codigo(a.codigo),
                  Expanded(child: Text(a.nome)),
                  if (!a.emUso) const Etiqueta('Descontinuado', fundo: Cores.alertaFundo, cor: Cores.alerta),
                ]),
            ]),
            _Bloco('Controles (Anexo A)', [
              for (final c in risco.controles)
                Row(crossAxisAlignment: CrossAxisAlignment.start, spacing: 8, children: [
                  SeloControle(c.codigo),
                  Expanded(child: Text(c.nome, style: const TextStyle(fontSize: 13.5))),
                ]),
            ]),
            if (risco.temPlano) _Plano(risco.id),
          ]),
        ],
      ),
    );
  }
}

class _Plano extends ConsumerWidget {
  const _Plano(this.riscoId);

  final int riscoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final plano = ref.watch(planoDoRiscoProvider(riscoId));
    return plano.when(
      loading: () => const Center(child: Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator())),
      error: (e, _) => EstadoErro(e, aoTentar: () => ref.invalidate(planoDoRiscoProvider(riscoId))),
      data: (p) => _Bloco('Plano de ação · ${p.concluidas}/${p.validas} concluídas', [
        if (p.valorResidual != null)
          Row(children: [
            const Text('Residual esperado', style: TextStyle(color: Cores.ink3, fontSize: 13)),
            const Spacer(),
            SeloNivel(p.nivelResidual!, valor: p.valorResidual),
          ]),
        if (p.aceiteVigente != null)
          Aviso.info('Aceito por ${p.aceiteVigente!.aceitoPorNome} até ${dataBr(p.aceiteVigente!.validoAte.toLocal())}.')
        else if (p.prontoParaAceite)
          const Aviso.alerta('Plano concluído: aguardando o aceite formal do Gerente.'),
        if (p.acoes.isEmpty) const Text('Nenhuma ação cadastrada.', style: TextStyle(color: Cores.ink3)),
        for (final a in p.acoes) CartaoAcao((acao: a, plano: p), dentroDoRisco: true),
      ]),
    );
  }
}

class _Bloco extends StatelessWidget {
  const _Bloco(this.titulo, this.filhos);

  final String titulo;
  final List<Widget> filhos;

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, spacing: 8, children: [
      Text(titulo.toUpperCase(),
          style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: Cores.ink3, letterSpacing: .8)),
      ...filhos,
    ]);
  }
}

class _Campo extends StatelessWidget {
  const _Campo(this.rotulo, this.valor, {this.empilhado = false});

  final String rotulo;
  final String valor;
  final bool empilhado;

  @override
  Widget build(BuildContext context) {
    const estiloRotulo = TextStyle(color: Cores.ink3, fontSize: 13);
    if (empilhado) {
      return Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 2, children: [
        Text(rotulo, style: estiloRotulo),
        Text(valor, style: const TextStyle(height: 1.35)),
      ]);
    }
    return Row(children: [
      Text(rotulo, style: estiloRotulo),
      const SizedBox(width: 12),
      Expanded(child: Text(valor, textAlign: TextAlign.right)),
    ]);
  }
}
