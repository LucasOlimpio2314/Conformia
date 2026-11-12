import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'regras_senha.dart';

/// Formulário de nova senha. Usado na troca obrigatória (sem senha atual) e
/// no "Alterar senha" da Conta (pede a senha atual).
class FormularioSenha extends StatefulWidget {
  const FormularioSenha({
    super.key,
    required this.pedirAtual,
    required this.textoBotao,
    required this.aoSalvar,
    this.topo,
  });

  final bool pedirAtual;
  final String textoBotao;
  final Widget? topo;

  /// Recebe (senhaAtual, novaSenha, confirmacao). Lança [ApiException] se o servidor recusar.
  final Future<void> Function(String atual, String nova, String confirmacao) aoSalvar;

  @override
  State<FormularioSenha> createState() => _FormularioSenhaState();
}

class _FormularioSenhaState extends State<FormularioSenha> {
  final _atual = TextEditingController();
  final _nova = TextEditingController();
  final _confirmacao = TextEditingController();
  bool _mostrar = false;
  bool _enviando = false;
  String? _erro;
  Map<String, String> _campos = const {};

  @override
  void initState() {
    super.initState();
    _nova.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _atual.dispose();
    _nova.dispose();
    _confirmacao.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    setState(() {
      _enviando = true;
      _erro = null;
      _campos = const {};
    });
    try {
      await widget.aoSalvar(_atual.text, _nova.text, _confirmacao.text);
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
    final regras = RegrasSenha(_nova.text);
    final olho = TextButton(
      onPressed: () => setState(() => _mostrar = !_mostrar),
      child: Text(_mostrar ? 'Ocultar' : 'Mostrar'),
    );
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          spacing: 14,
          children: [
            ?widget.topo,
            if (_erro != null) Aviso.erro(_erro!),
            if (widget.pedirAtual)
              TextField(
                controller: _atual,
                obscureText: !_mostrar,
                decoration: InputDecoration(labelText: 'Senha atual', errorText: _campos['senhaAtual']),
              ),
            TextField(
              key: const Key('senha-nova'),
              controller: _nova,
              obscureText: !_mostrar,
              decoration: InputDecoration(labelText: 'Nova senha', errorText: _campos['novaSenha'], suffixIcon: olho),
            ),
            Column(crossAxisAlignment: CrossAxisAlignment.start, spacing: 3, children: [
              _Regra('8 caracteres ou mais', regras.tamanho),
              _Regra('letras', regras.letra),
              _Regra('pelo menos um número', regras.numero),
            ]),
            TextField(
              key: const Key('senha-confirmacao'),
              controller: _confirmacao,
              obscureText: !_mostrar,
              onSubmitted: (_) => _salvar(),
              decoration: InputDecoration(labelText: 'Confirmar nova senha', errorText: _campos['confirmacao']),
            ),
            const SizedBox(height: 4),
            FilledButton(
              key: const Key('senha-salvar'),
              onPressed: _enviando ? null : _salvar,
              child: _enviando
                  ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : Text(widget.textoBotao),
            ),
          ],
        ),
      ],
    );
  }
}

class _Regra extends StatelessWidget {
  const _Regra(this.texto, this.ok);

  final String texto;
  final bool ok;

  @override
  Widget build(BuildContext context) {
    return Text('${ok ? '✓' : '○'}  $texto',
        style: TextStyle(fontSize: 12.5, color: ok ? Cores.ok : Cores.ink3, fontWeight: ok ? FontWeight.w500 : null));
  }
}
