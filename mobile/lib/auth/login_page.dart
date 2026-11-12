import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import 'auth_controller.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _email = TextEditingController();
  final _senha = TextEditingController();
  bool _mostrarSenha = false;
  bool _enviando = false;
  String? _erro;

  @override
  void dispose() {
    _email.dispose();
    _senha.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    if (_email.text.trim().isEmpty || _senha.text.isEmpty) {
      setState(() => _erro = 'Informe o e-mail e a senha.');
      return;
    }
    setState(() {
      _enviando = true;
      _erro = null;
    });
    try {
      await ref.read(authProvider.notifier).entrar(_email.text, _senha.text);
    } on ApiException catch (e) {
      if (mounted) setState(() => _erro = e.mensagem);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Cores.superficie,
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          const _Marca(),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 24),
            child: AutofillGroup(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                spacing: 14,
                children: [
                  if (_erro != null) Aviso.erro(_erro!),
                  TextField(
                    key: const Key('login-email'),
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.username],
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(labelText: 'E-mail'),
                  ),
                  TextField(
                    key: const Key('login-senha'),
                    controller: _senha,
                    obscureText: !_mostrarSenha,
                    autofillHints: const [AutofillHints.password],
                    onSubmitted: (_) => _entrar(),
                    decoration: InputDecoration(
                      labelText: 'Senha',
                      suffixIcon: TextButton(
                        onPressed: () => setState(() => _mostrarSenha = !_mostrarSenha),
                        child: Text(_mostrarSenha ? 'Ocultar' : 'Mostrar'),
                      ),
                    ),
                  ),
                  FilledButton(
                    key: const Key('login-entrar'),
                    onPressed: _enviando ? null : _entrar,
                    child: _enviando
                        ? const SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Text('Entrar'),
                  ),
                  const Text(
                    'Esqueceu a senha? Peça a um gerente para gerar uma senha temporária.',
                    style: TextStyle(color: Cores.ink3, fontSize: 12.5),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Faixa índigo do topo, igual ao painel da esquerda do login web.
class _Marca extends StatelessWidget {
  const _Marca();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Cores.indigo,
      padding: EdgeInsets.fromLTRB(20, MediaQuery.paddingOf(context).top + 28, 20, 30),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        spacing: 16,
        children: [
          Row(spacing: 10, children: [
            Container(
              width: 30,
              height: 30,
              alignment: Alignment.center,
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(7)),
              child: const Text('27k',
                  style: TextStyle(color: Cores.indigo, fontFamily: 'monospace', fontSize: 12, fontWeight: FontWeight.w600)),
            ),
            const Text('Conformia', style: TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w700)),
          ]),
          const Text('Registros ISO 27001\nna palma da mão',
              style: TextStyle(color: Colors.white, fontSize: 22, height: 1.25, fontWeight: FontWeight.w600)),
          Wrap(spacing: 6, runSpacing: 6, children: [
            for (final c in ['6.1.2', '6.1.3', '8.2', '8.3', 'Anexo A'])
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.white38),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(c, style: const TextStyle(color: Color(0xFFE7E9FB), fontSize: 11, fontFamily: 'monospace')),
              ),
          ]),
        ],
      ),
    );
  }
}
