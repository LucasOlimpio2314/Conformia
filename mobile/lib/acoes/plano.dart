import '../core/formatos.dart';
import '../aceites/aceite.dart';
import '../riscos/risco.dart';

/// Espelho do `AcaoResumo` (docs/api/plano-aceite.md).
class Acao {
  const Acao({
    required this.id,
    required this.codigo,
    required this.riscoId,
    required this.titulo,
    required this.responsavelId,
    required this.responsavelNome,
    required this.prazo,
    required this.controle,
    required this.situacao,
    required this.atrasada,
    this.evidencia,
  });

  final int id;
  final String codigo;
  final int riscoId;
  final String titulo;
  final int responsavelId;
  final String responsavelNome;
  final DateTime prazo;
  final Controle controle;

  /// PENDENTE, EM_ANDAMENTO, CONCLUIDA ou CANCELADA.
  final String situacao;
  final String? evidencia;

  /// Calculado pelo servidor: em aberto com prazo vencido.
  final bool atrasada;

  /// Concluída e cancelada não mudam mais (regra do servidor).
  bool get aberta => situacao == 'PENDENTE' || situacao == 'EM_ANDAMENTO';

  int diasDeAtraso(DateTime hoje) => DateTime(hoje.year, hoje.month, hoje.day).difference(prazo).inDays;

  factory Acao.deJson(Map<String, dynamic> j) => Acao(
        id: (j['id'] as num).toInt(),
        codigo: j['codigo'] as String,
        riscoId: (j['riscoId'] as num).toInt(),
        titulo: j['titulo'] as String,
        responsavelId: (j['responsavelId'] as num).toInt(),
        responsavelNome: j['responsavelNome'] as String,
        prazo: DateTime.parse(j['prazo'] as String),
        controle: Controle.deJson(j['controle'] as Map<String, dynamic>),
        situacao: j['situacao'] as String,
        evidencia: j['evidencia'] as String?,
        atrasada: j['atrasada'] == true,
      );

  /// Corpo do `PUT /api/plano/acoes/{id}`: a API pede o formulário inteiro, então
  /// mandamos de volta o que já existe e mudamos só a situação e a evidência.
  Map<String, dynamic> paraForm({required String situacao, required String? evidencia}) => {
        'titulo': titulo,
        'responsavelId': responsavelId,
        'prazo': dataApi(prazo),
        'controle': controle.codigo,
        'situacao': situacao,
        'evidencia': evidencia,
      };
}

const situacoesAcao = {
  'PENDENTE': 'Pendente',
  'EM_ANDAMENTO': 'Em andamento',
  'CONCLUIDA': 'Concluída',
  'CANCELADA': 'Cancelada',
};

/// Espelho do `PlanoRisco`: o plano de ação de um risco.
class PlanoRisco {
  const PlanoRisco({
    required this.riscoId,
    required this.codigo,
    required this.titulo,
    required this.valor,
    required this.nivel,
    required this.acoes,
    required this.concluidas,
    required this.validas,
    required this.prontoParaAceite,
    this.valorResidual,
    this.nivelResidual,
    this.aceiteVigente,
  });

  final int riscoId;
  final String codigo;
  final String titulo;
  final int valor;
  final String nivel;
  final int? valorResidual;
  final String? nivelResidual;
  final List<Acao> acoes;
  final int concluidas;
  final int validas;
  final bool prontoParaAceite;
  final AceiteResumo? aceiteVigente;

  factory PlanoRisco.deJson(Map<String, dynamic> j) => PlanoRisco(
        riscoId: (j['riscoId'] as num).toInt(),
        codigo: j['codigo'] as String,
        titulo: j['titulo'] as String,
        valor: (j['valor'] as num).toInt(),
        nivel: j['nivel'] as String,
        valorResidual: (j['valorResidual'] as num?)?.toInt(),
        nivelResidual: j['nivelResidual'] as String?,
        acoes: [for (final a in j['acoes'] as List) Acao.deJson(a as Map<String, dynamic>)],
        concluidas: (j['concluidas'] as num).toInt(),
        validas: (j['validas'] as num).toInt(),
        prontoParaAceite: j['prontoParaAceite'] == true,
        aceiteVigente:
            j['aceiteVigente'] == null ? null : AceiteResumo.deJson(j['aceiteVigente'] as Map<String, dynamic>),
      );
}

/// Uma ação junto com o risco a que pertence (para mostrar "RSK-003 · título").
typedef MinhaAcao = ({Acao acao, PlanoRisco plano});

/// Ações em que [usuarioId] é o responsável, em ordem de urgência:
/// atrasadas primeiro, depois as de prazo mais próximo.
List<MinhaAcao> minhasAcoes(List<PlanoRisco> planos, int usuarioId) {
  final lista = <MinhaAcao>[
    for (final p in planos)
      for (final a in p.acoes)
        if (a.responsavelId == usuarioId) (acao: a, plano: p),
  ];
  lista.sort((x, y) {
    if (x.acao.atrasada != y.acao.atrasada) return x.acao.atrasada ? -1 : 1;
    return x.acao.prazo.compareTo(y.acao.prazo);
  });
  return lista;
}
