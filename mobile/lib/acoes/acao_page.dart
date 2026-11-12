import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../auth/auth_controller.dart';
import '../core/api_client.dart';
import '../core/formatos.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'acoes_providers.dart';
import 'minhas_acoes_page.dart';
import 'plano.dart';

/// Detalhe da ação. No celular o responsável só muda a situação e registra a
/// evidência; prazo, controle e responsável são definidos na web (planejamento).
class AcaoPage extends ConsumerStatefulWidget {
  const AcaoPage(this.item, {super.key});

  final MinhaAcao item;

  @override
  ConsumerState<AcaoPage> createState() => _AcaoPageState();
}

class _AcaoPageState extends ConsumerState<AcaoPage> {
  late String _situacao = widget.item.acao.situacao;
  late final _evidencia = TextEditingController(text: widget.item.acao.evidencia ?? '');
  bool _enviando = false;
  String? _erro;
  Map<String, String> _campos = const {};

  Acao get _acao => widget.item.acao;

  @override
  void dispose() {
    _evidencia.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    // Mesma regra do servidor, avisada antes para poupar uma ida à API.
    if (_situacao == 'CONCLUIDA' && _evidencia.text.trim().isEmpty) {
      setState(() => _campos = {'evidencia': 'Para concluir, registre a evidência.'});
      return;
    }
    setState(() {
      _enviando = true;
      _erro = null;
      _campos = const {};
    });
    try {
      final evidencia = _evidencia.text.trim();
      await salvarAcao(ref, _acao, situacao: _situacao, evidencia: evidencia.isEmpty ? null : evidencia);
      if (!mounted) return;
      mostrarMensagem(context, '${_acao.codigo} salva');
      Navigator.of(context).pop();
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _campos = e.campos;
          _erro = e.campos.isEmpty ? e.mensagem : null;
        });
      }
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final usuario = ref.watch(authProvider).value?.usuario;
    final editavel = _acao.aberta && (usuario?.podeAlterar ?? false);
    final plano = widget.item.plano;

    return Scaffold(
      appBar: AppBar(title: Text(_acao.codigo)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Column(crossAxisAlignment: CrossAxisAlignment.stretch, spacing: 14, children: [
            if (_erro != null) Aviso.erro(_erro!),
            Text(_acao.titulo, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600, height: 1.3)),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(spacing: 8, children: [
                  _Linha('Risco', Row(mainAxisSize: MainAxisSize.min, spacing: 6, children: [
                    Codigo(plano.codigo),
                    SeloNivel(plano.nivel, valor: plano.valor),
                  ])),
                  _Linha('', Text(plano.titulo, textAlign: TextAlign.right, style: const TextStyle(color: Cores.ink2))),
                  _Linha('Controle', Row(mainAxisSize: MainAxisSize.min, spacing: 6, children: [
                    SeloControle(_acao.controle.codigo),
                    Flexible(child: Text(_acao.controle.nome, overflow: TextOverflow.ellipsis)),
                  ])),
                  _Linha(
                    'Prazo',
                    Text(
                      _acao.atrasada ? '${dataBr(_acao.prazo)} · atrasada' : dataBr(_acao.prazo),
                      style: TextStyle(
                        color: _acao.atrasada ? Cores.erro : Cores.ink,
                        fontWeight: _acao.atrasada ? FontWeight.w600 : null,
                      ),
                    ),
                  ),
                  _Linha('Situação', SeloSituacao(_acao, hoje: DateTime.now())),
                ]),
              ),
            ),
            if (editavel) ...[
              const _Titulo('Atualizar situação'),
              RadioGroup<String>(
                groupValue: _situacao,
                onChanged: (v) => setState(() => _situacao = v!),
                child: Column(children: [
                  for (final s in const ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA'])
                    RadioListTile<String>(
                      key: Key('situacao-$s'),
                      value: s,
                      title: Text(situacoesAcao[s]!),
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                    ),
                ]),
              ),
              TextField(
                key: const Key('acao-evidencia'),
                controller: _evidencia,
                // Some com o erro assim que a pessoa começa a corrigir.
                onChanged: (_) {
                  if (_campos.containsKey('evidencia')) setState(() => _campos = const {});
                },
                minLines: 3,
                maxLines: 6,
                maxLength: 1000,
                decoration: InputDecoration(
                  labelText: _situacao == 'CONCLUIDA' ? 'Evidência *' : 'Evidência (opcional)',
                  alignLabelWithHint: true,
                  helperText: 'Obrigatória para concluir. Depois de concluída, a ação não muda mais.',
                  helperMaxLines: 2,
                  errorText: _campos['evidencia'],
                ),
              ),
              FilledButton(
                key: const Key('acao-salvar'),
                onPressed: _enviando ? null : _salvar,
                child: _enviando
                    ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Salvar'),
              ),
            ] else ...[
              if (_acao.evidencia != null) ...[
                const _Titulo('Evidência'),
                Text(_acao.evidencia!, style: const TextStyle(color: Cores.ink2, height: 1.4)),
              ],
              if (!_acao.aberta) const Aviso.info('Ação finalizada: não pode mais ser alterada.'),
            ],
          ]),
        ],
      ),
    );
  }
}

class _Linha extends StatelessWidget {
  const _Linha(this.rotulo, this.valor);

  final String rotulo;
  final Widget valor;

  @override
  Widget build(BuildContext context) {
    return Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SizedBox(width: 80, child: Text(rotulo, style: const TextStyle(color: Cores.ink3, fontSize: 13))),
      Expanded(child: Align(alignment: Alignment.centerRight, child: valor)),
    ]);
  }
}

class _Titulo extends StatelessWidget {
  const _Titulo(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) => Text(texto.toUpperCase(),
      style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: Cores.ink3, letterSpacing: .8));
}
