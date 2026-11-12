import 'package:material_ui/material_ui.dart';

import 'api_client.dart';
import 'tema.dart';

/// Nível do risco com as mesmas cores da matriz 5×5 da web.
class SeloNivel extends StatelessWidget {
  const SeloNivel(this.nivel, {super.key, this.valor});

  final String nivel;
  final int? valor;

  static const _cores = {
    'BAIXO': (Color(0xFFCFE8D9), Color(0xFF14532D), 'Baixo'),
    'MEDIO': (Color(0xFFF6E3A1), Color(0xFF6B4E00), 'Médio'),
    'ALTO': (Color(0xFFF5B98A), Color(0xFF7A2E0B), 'Alto'),
    'CRITICO': (Color(0xFFD9534A), Color(0xFFFFFFFF), 'Crítico'),
  };

  @override
  Widget build(BuildContext context) {
    final (fundo, texto, rotulo) = _cores[nivel] ?? (Cores.linha2, Cores.ink2, nivel);
    return Etiqueta(valor == null ? rotulo : '$rotulo  $valor', fundo: fundo, cor: texto);
  }
}

/// Etiqueta pequena (situação, prazo, perfil).
class Etiqueta extends StatelessWidget {
  const Etiqueta(this.texto, {super.key, required this.fundo, required this.cor});

  final String texto;
  final Color fundo;
  final Color cor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(color: fundo, borderRadius: BorderRadius.circular(4)),
      child: Text(texto, style: TextStyle(color: cor, fontSize: 11.5, fontWeight: FontWeight.w600)),
    );
  }
}

/// Código de controle do Anexo A (ex.: 8.13).
class SeloControle extends StatelessWidget {
  const SeloControle(this.codigo, {super.key});

  final String codigo;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
      decoration: BoxDecoration(color: Cores.indigo50, borderRadius: BorderRadius.circular(4)),
      child: Text(codigo,
          style: const TextStyle(color: Cores.indigo, fontSize: 11.5, fontFamily: 'monospace')),
    );
  }
}

class Codigo extends StatelessWidget {
  const Codigo(this.texto, {super.key});

  final String texto;

  @override
  Widget build(BuildContext context) =>
      Text(texto, style: const TextStyle(fontFamily: 'monospace', fontSize: 12, color: Cores.ink2));
}

/// Caixa de mensagem (informação, alerta ou erro).
class Aviso extends StatelessWidget {
  const Aviso.info(this.texto, {super.key})
      : fundo = Cores.indigo50,
        cor = Cores.indigo;
  const Aviso.alerta(this.texto, {super.key})
      : fundo = Cores.alertaFundo,
        cor = Cores.alerta;
  const Aviso.erro(this.texto, {super.key})
      : fundo = Cores.erroFundo,
        cor = Cores.erro;

  final String texto;
  final Color fundo;
  final Color cor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(color: fundo, borderRadius: BorderRadius.circular(8)),
      child: Text(texto, style: TextStyle(color: cor, fontSize: 13, height: 1.35)),
    );
  }
}

/// Tela de erro com botão "Tentar de novo" (usada quando uma lista não carrega).
class EstadoErro extends StatelessWidget {
  const EstadoErro(this.erro, {super.key, required this.aoTentar});

  final Object erro;
  final VoidCallback aoTentar;

  @override
  Widget build(BuildContext context) {
    final texto = erro is ApiException ? (erro as ApiException).mensagem : 'Erro inesperado: $erro';
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, spacing: 12, children: [
          const Icon(Icons.cloud_off_outlined, color: Cores.ink3, size: 36),
          Text(texto, textAlign: TextAlign.center, style: const TextStyle(color: Cores.ink2)),
          OutlinedButton(onPressed: aoTentar, child: const Text('Tentar de novo')),
        ]),
      ),
    );
  }
}

class Vazio extends StatelessWidget {
  const Vazio(this.texto, {super.key, this.icone = Icons.inbox_outlined});

  final String texto;
  final IconData icone;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, spacing: 8, children: [
          Icon(icone, color: Cores.ink3, size: 36),
          Text(texto, textAlign: TextAlign.center, style: const TextStyle(color: Cores.ink3)),
        ]),
      ),
    );
  }
}

/// Linha de filtros (Abertas 2 · Atrasadas 1 ...). Compacta e com rolagem
/// lateral, para caber numa linha só mesmo em telas estreitas.
class Filtros extends StatelessWidget {
  const Filtros({super.key, required this.opcoes, required this.selecionada, required this.aoSelecionar});

  /// (id, rótulo, quantidade)
  final List<(String, String, int?)> opcoes;
  final String? selecionada;
  final void Function(String id) aoSelecionar;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(spacing: 6, children: [
        for (final (id, rotulo, quantidade) in opcoes)
          ChoiceChip(
            key: Key('filtro-$id'),
            label: Text(quantidade == null ? rotulo : '$rotulo  $quantidade'),
            selected: selecionada == id,
            showCheckmark: false,
            visualDensity: VisualDensity.compact,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            labelPadding: const EdgeInsets.symmetric(horizontal: 4),
            labelStyle: TextStyle(
              fontSize: 13,
              color: selecionada == id ? Colors.white : Cores.ink2,
              fontWeight: selecionada == id ? FontWeight.w600 : FontWeight.w400,
            ),
            selectedColor: Cores.indigo,
            backgroundColor: Cores.superficie,
            side: BorderSide(color: selecionada == id ? Cores.indigo : Cores.linha),
            shape: const StadiumBorder(),
            onSelected: (_) => aoSelecionar(id),
          ),
      ]),
    );
  }
}

/// Mostra uma mensagem rápida no rodapé (ex.: "Ação salva").
void mostrarMensagem(BuildContext context, String texto) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(texto), behavior: SnackBarBehavior.floating));
}
