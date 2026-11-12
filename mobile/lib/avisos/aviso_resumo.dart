/// Espelho do `AvisoResumo` (docs/api/plano-aceite.md). Hoje só Gerentes recebem.
class AvisoResumo {
  const AvisoResumo({
    required this.id,
    required this.tipo,
    required this.titulo,
    required this.mensagem,
    required this.criadoEm,
    required this.lido,
    this.link,
  });

  final int id;

  /// ACEITE_INVALIDADO, ACEITE_VENCIDO ou RESIDUAL_PRONTO.
  final String tipo;
  final String titulo;
  final String mensagem;

  /// Caminho da tela na web (hoje sempre `/aceites`). No app vira a aba Aceites.
  final String? link;
  final DateTime criadoEm;
  final bool lido;

  factory AvisoResumo.deJson(Map<String, dynamic> j) => AvisoResumo(
        id: (j['id'] as num).toInt(),
        tipo: j['tipo'] as String,
        titulo: j['titulo'] as String,
        mensagem: j['mensagem'] as String,
        link: j['link'] as String?,
        criadoEm: DateTime.parse(j['criadoEm'] as String),
        lido: j['lido'] == true,
      );
}

/// Avisos que ainda não viraram notificação no celular: não lidos e com id maior
/// que o último já notificado. Os ids do banco só crescem, então guardar o maior
/// basta para nunca notificar o mesmo aviso duas vezes.
List<AvisoResumo> avisosParaNotificar(List<AvisoResumo> avisos, int ultimoNotificado) {
  final novos = avisos.where((a) => !a.lido && a.id > ultimoNotificado).toList()
    ..sort((a, b) => a.id.compareTo(b.id));
  return novos;
}
