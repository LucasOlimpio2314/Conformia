import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../core/widgets.dart';
import 'auth_controller.dart';
import 'formulario_senha.dart';

/// Aparece sozinha quando o login foi feito com senha temporária.
/// Não tem "voltar": ou troca a senha, ou sai.
class TrocarSenhaPage extends ConsumerWidget {
  const TrocarSenhaPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.read(authProvider.notifier);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Defina sua senha'),
        automaticallyImplyLeading: false,
        actions: [TextButton(onPressed: auth.sair, child: const Text('Sair'))],
      ),
      body: FormularioSenha(
        pedirAtual: false,
        textoBotao: 'Salvar e continuar',
        topo: const Aviso.info('Você entrou com uma senha temporária. Crie a sua para continuar.'),
        aoSalvar: (_, nova, confirmacao) => auth.trocarSenhaObrigatoria(nova, confirmacao),
      ),
    );
  }
}
