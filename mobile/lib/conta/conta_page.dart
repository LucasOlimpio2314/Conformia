import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../auth/auth_controller.dart';
import '../auth/formulario_senha.dart';
import '../core/config.dart';
import '../core/formatos.dart';
import '../core/providers.dart';
import '../core/tema.dart';
import '../core/widgets.dart';
import '../notificacoes/notificador.dart';

/// Igual ao "Minha conta" da web: dados do usuário, alterar a própria senha e sair.
class ContaPage extends ConsumerWidget {
  const ContaPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(authProvider).value!.usuario;
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        Column(crossAxisAlignment: CrossAxisAlignment.stretch, spacing: 10, children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(spacing: 12, children: [
                CircleAvatar(
                  radius: 22,
                  backgroundColor: Cores.indigo50,
                  child: Text(usuario.iniciais,
                      style: const TextStyle(color: Cores.indigo, fontWeight: FontWeight.w600)),
                ),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(usuario.nome, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                    Text(usuario.email, style: const TextStyle(color: Cores.ink3, fontSize: 13)),
                  ]),
                ),
              ]),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(spacing: 8, children: [
                _Linha('Perfil', Etiqueta(usuario.perfilRotulo, fundo: Cores.indigo50, cor: Cores.indigo)),
                if (usuario.ultimoAcesso != null) _Linha('Último acesso', Text(quando(usuario.ultimoAcesso!))),
                _Linha('Servidor', const Codigo(apiUrl)),
              ]),
            ),
          ),
          OutlinedButton(
            key: const Key('conta-alterar-senha'),
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AlterarSenhaPage())),
            child: const Text('Alterar senha'),
          ),
          if (usuario.gerente) const _BotaoNotificacoes(),
          const SizedBox(height: 16),
          OutlinedButton(
            key: const Key('conta-sair'),
            style: OutlinedButton.styleFrom(foregroundColor: Cores.erro),
            onPressed: () => ref.read(authProvider.notifier).sair(),
            child: const Text('Sair'),
          ),
        ]),
      ],
    );
  }
}

/// Mostra se o Android está deixando o app notificar; se não, pede de novo.
class _BotaoNotificacoes extends StatefulWidget {
  const _BotaoNotificacoes();

  @override
  State<_BotaoNotificacoes> createState() => _BotaoNotificacoesState();
}

class _BotaoNotificacoesState extends State<_BotaoNotificacoes> {
  bool? _ativas;

  @override
  void initState() {
    super.initState();
    Notificador.permitido().then((v) {
      if (mounted) setState(() => _ativas = v);
    });
  }

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: _ativas == true
          ? null
          : () async {
              final ok = await Notificador.pedirPermissao();
              if (mounted) setState(() => _ativas = ok);
            },
      child: Text(switch (_ativas) {
        null => 'Notificações…',
        true => 'Notificações: ativadas',
        false => 'Ativar notificações',
      }),
    );
  }
}

class AlterarSenhaPage extends ConsumerWidget {
  const AlterarSenhaPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Alterar senha')),
      body: FormularioSenha(
        pedirAtual: true,
        textoBotao: 'Salvar nova senha',
        aoSalvar: (atual, nova, confirmacao) async {
          await ref.read(apiProvider).put('/api/minha-conta/senha', {
            'senhaAtual': atual,
            'novaSenha': nova,
            'confirmacao': confirmacao,
          });
          if (!context.mounted) return;
          mostrarMensagem(context, 'Senha alterada');
          Navigator.of(context).pop();
        },
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
    return Row(children: [
      Text(rotulo, style: const TextStyle(color: Cores.ink3, fontSize: 13)),
      const Spacer(),
      valor,
    ]);
  }
}
