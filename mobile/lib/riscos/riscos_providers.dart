import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../acoes/plano.dart';
import '../core/providers.dart';
import 'risco.dart';

/// `GET /api/riscos` — já vem ordenado do maior nível para o menor.
final riscosProvider = FutureProvider.autoDispose<List<Risco>>((ref) async {
  final json = await ref.read(apiProvider).get('/api/riscos') as List;
  return [for (final r in json) Risco.deJson(r as Map<String, dynamic>)];
});

/// Plano de ação de um risco (`GET /api/plano/{riscoId}`), usado no detalhe.
final planoDoRiscoProvider = FutureProvider.autoDispose.family<PlanoRisco, int>((ref, riscoId) async {
  final json = await ref.read(apiProvider).get('/api/plano/$riscoId');
  return PlanoRisco.deJson(json as Map<String, dynamic>);
});
