import '../riscos/risco.dart';

/// Espelho do `AceiteResumo` (docs/api/plano-aceite.md).
class AceiteResumo {
  const AceiteResumo({
    required this.id,
    required this.tipo,
    required this.valor,
    required this.nivel,
    required this.situacao,
    required this.aceitoPorNome,
    required this.validoAte,
    this.motivoEncerramento,
    this.encerradoEm,
  });

  final int id;

  /// RISCO (tratamento Aceitar) ou RESIDUAL (depois do plano).
  final String tipo;
  final int valor;
  final String nivel;

  /// VIGENTE, VENCIDO, REVOGADO ou INVALIDADO.
  final String situacao;
  final String aceitoPorNome;
  final DateTime validoAte;
  final String? motivoEncerramento;
  final DateTime? encerradoEm;

  factory AceiteResumo.deJson(Map<String, dynamic> j) => AceiteResumo(
        id: (j['id'] as num).toInt(),
        tipo: j['tipo'] as String,
        valor: (j['valor'] as num).toInt(),
        nivel: j['nivel'] as String,
        situacao: j['situacao'] as String,
        aceitoPorNome: j['aceitoPorNome'] as String? ?? '',
        validoAte: DateTime.parse(j['validoAte'] as String),
        motivoEncerramento: j['motivoEncerramento'] as String?,
        encerradoEm: j['encerradoEm'] == null ? null : DateTime.parse(j['encerradoEm'] as String),
      );
}

/// Espelho do `PendenciaAceite`: um risco esperando o aceite do Gerente.
class PendenciaAceite {
  const PendenciaAceite({
    required this.risco,
    required this.tipo,
    required this.valor,
    required this.nivel,
    required this.acoesConcluidas,
    this.anterior,
  });

  final Risco risco;
  final String tipo;

  /// Nível que o Gerente está aceitando (inerente se RISCO, residual se RESIDUAL).
  final int valor;
  final String nivel;
  final int acoesConcluidas;

  /// Último aceite, quando ele caiu (para explicar por que o risco voltou a pedir aceite).
  final AceiteResumo? anterior;

  bool get residual => tipo == 'RESIDUAL';

  factory PendenciaAceite.deJson(Map<String, dynamic> j) => PendenciaAceite(
        risco: Risco.deJson(j['risco'] as Map<String, dynamic>),
        tipo: j['tipo'] as String,
        valor: (j['valor'] as num).toInt(),
        nivel: j['nivel'] as String,
        acoesConcluidas: (j['acoesConcluidas'] as num?)?.toInt() ?? 0,
        anterior: j['anterior'] == null ? null : AceiteResumo.deJson(j['anterior'] as Map<String, dynamic>),
      );
}
