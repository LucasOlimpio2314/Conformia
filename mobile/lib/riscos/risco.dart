/// Controle do Anexo A (ex.: 8.13 – Backup de informações).
class Controle {
  const Controle(this.codigo, this.nome);

  final String codigo;
  final String nome;

  factory Controle.deJson(Map<String, dynamic> j) => Controle(j['codigo'] as String, j['nome'] as String? ?? '');
}

/// Ativo ligado ao risco (resumido).
class AtivoDoRisco {
  const AtivoDoRisco(this.codigo, this.nome, this.emUso);

  final String codigo;
  final String nome;
  final bool emUso;

  factory AtivoDoRisco.deJson(Map<String, dynamic> j) =>
      AtivoDoRisco(j['codigo'] as String, j['nome'] as String, j['emUso'] != false);
}

/// Espelho do `RiscoResumo` (docs/api/riscos.md).
class Risco {
  const Risco({
    required this.id,
    required this.codigo,
    required this.titulo,
    required this.ameaca,
    required this.vulnerabilidade,
    required this.ativos,
    required this.controles,
    required this.donoNome,
    required this.probabilidade,
    required this.impacto,
    required this.valor,
    required this.nivel,
    required this.situacao,
    this.cenario,
    this.tratamento,
  });

  final int id;
  final String codigo;
  final String titulo;
  final String? cenario;
  final String ameaca;
  final String vulnerabilidade;
  final List<AtivoDoRisco> ativos;
  final List<Controle> controles;
  final String donoNome;
  final int probabilidade;
  final int impacto;
  final int valor;

  /// BAIXO, MEDIO, ALTO ou CRITICO.
  final String nivel;

  /// MITIGAR, ACEITAR, TRANSFERIR, EVITAR ou null ("a definir").
  final String? tratamento;

  /// ATIVO ou ENCERRADO.
  final String situacao;

  bool get ativo => situacao == 'ATIVO';

  /// Só estes tratamentos têm plano de ação.
  bool get temPlano => const {'MITIGAR', 'TRANSFERIR', 'EVITAR'}.contains(tratamento);

  String get tratamentoRotulo => switch (tratamento) {
        'MITIGAR' => 'Mitigar',
        'ACEITAR' => 'Aceitar',
        'TRANSFERIR' => 'Transferir',
        'EVITAR' => 'Evitar',
        _ => 'Tratamento a definir',
      };

  /// Busca simples por código, título, ameaça ou ativo.
  bool combina(String termo) {
    final t = termo.trim().toLowerCase();
    if (t.isEmpty) return true;
    return codigo.toLowerCase().contains(t) ||
        titulo.toLowerCase().contains(t) ||
        ameaca.toLowerCase().contains(t) ||
        ativos.any((a) => a.nome.toLowerCase().contains(t) || a.codigo.toLowerCase().contains(t));
  }

  factory Risco.deJson(Map<String, dynamic> j) => Risco(
        id: (j['id'] as num).toInt(),
        codigo: j['codigo'] as String,
        titulo: j['titulo'] as String,
        cenario: j['cenario'] as String?,
        ameaca: (j['ameaca'] as Map?)?['nome'] as String? ?? '',
        vulnerabilidade: (j['vulnerabilidade'] as Map?)?['nome'] as String? ?? '',
        ativos: [for (final a in j['ativos'] as List? ?? const []) AtivoDoRisco.deJson(a as Map<String, dynamic>)],
        controles: [for (final c in j['controles'] as List? ?? const []) Controle.deJson(c as Map<String, dynamic>)],
        donoNome: j['donoNome'] as String? ?? '',
        probabilidade: (j['probabilidade'] as num).toInt(),
        impacto: (j['impacto'] as num).toInt(),
        valor: (j['valor'] as num).toInt(),
        nivel: j['nivel'] as String,
        tratamento: j['tratamento'] as String?,
        situacao: j['situacao'] as String? ?? 'ATIVO',
      );
}

const escalaProbabilidade = {1: 'Rara', 2: 'Improvável', 3: 'Possível', 4: 'Provável', 5: 'Quase certa'};
const escalaImpacto = {1: 'Insignificante', 2: 'Menor', 3: 'Moderado', 4: 'Maior', 5: 'Catastrófico'};
