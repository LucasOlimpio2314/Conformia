import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';
import '../core/formatos.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'aceite.dart';
import 'aceites_page.dart';
import 'aceites_providers.dart';

/// Aceite formal do risco (ISO 27001 6.1.3 f). Só o Gerente chega aqui.
class AceitePage extends ConsumerStatefulWidget {
  const AceitePage(this.pendencia, {super.key});

  final PendenciaAceite pendencia;

  @override
  ConsumerState<AceitePage> createState() => _AceitePageState();
}

class _AceitePageState extends ConsumerState<AceitePage> {
  final _justificativa = TextEditingController();
  final _condicoes = TextEditingController();
  bool _declaro = false;
  bool _enviando = false;
  String? _erro;
  Map<String, String> _campos = const {};

  @override
  void dispose() {
    _justificativa.dispose();
    _condicoes.dispose();
    super.dispose();
  }

  Future<void> _aceitar() async {
    setState(() {
      _enviando = true;
      _erro = null;
      _campos = const {};
    });
    try {
      final condicoes = _condicoes.text.trim();
      await registrarAceite(ref, widget.pendencia.risco.id,
          justificativa: _justificativa.text.trim(), condicoes: condicoes.isEmpty ? null : condicoes);
      if (!mounted) return;
      mostrarMensagem(context, 'Aceite de ${widget.pendencia.risco.codigo} registrado');
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
    final p = widget.pendencia;
    final anterior = p.anterior;
    return Scaffold(
      appBar: AppBar(title: const Text('Aceite formal')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Column(crossAxisAlignment: CrossAxisAlignment.stretch, spacing: 14, children: [
            CartaoPendencia(p, clicavel: false),
            if (anterior != null && anterior.motivoEncerramento != null)
              Aviso.alerta('O aceite anterior caiu'
                  '${anterior.encerradoEm == null ? '' : ' em ${dataBr(anterior.encerradoEm!.toLocal())}'}: '
                  '${anterior.motivoEncerramento}'),
            if (_erro != null) Aviso.erro(_erro!),
            TextField(
              key: const Key('aceite-justificativa'),
              controller: _justificativa,
              onChanged: (_) {
                if (_campos.containsKey('justificativa')) setState(() => _campos = const {});
              },
              minLines: 3,
              maxLines: 6,
              maxLength: 1000,
              decoration: InputDecoration(
                labelText: 'Justificativa *',
                alignLabelWithHint: true,
                helperText: 'Mínimo de 20 caracteres.',
                errorText: _campos['justificativa'],
              ),
            ),
            TextField(
              controller: _condicoes,
              minLines: 2,
              maxLines: 4,
              maxLength: 1000,
              decoration: InputDecoration(
                labelText: 'Condições (opcional)',
                hintText: 'Ex.: revisar após o teste de restauração',
                alignLabelWithHint: true,
                errorText: _campos['condicoes'],
              ),
            ),
            CheckboxListTile(
              key: const Key('aceite-declaracao'),
              value: _declaro,
              onChanged: (v) => setState(() => _declaro = v ?? false),
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
              title: Text(
                'Declaro que conheço o ${p.residual ? 'risco residual' : 'risco'} e o aceito '
                'em nome da organização por 12 meses.',
                style: const TextStyle(fontSize: 13.5, color: Cores.ink2),
              ),
            ),
            FilledButton(
              key: const Key('aceite-registrar'),
              onPressed: _declaro && !_enviando ? _aceitar : null,
              child: _enviando
                  ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Registrar aceite'),
            ),
          ]),
        ],
      ),
    );
  }
}
