import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../acoes/acoes_providers.dart';
import '../core/providers.dart';
import '../riscos/riscos_providers.dart';
import 'aceite.dart';

/// Riscos esperando o aceite do Gerente (`GET /api/aceites/pendencias`).
final pendenciasProvider = FutureProvider.autoDispose<List<PendenciaAceite>>((ref) async {
  final json = await ref.read(apiProvider).get('/api/aceites/pendencias') as List;
  return [for (final p in json) PendenciaAceite.deJson(p as Map<String, dynamic>)];
});

/// Registra o aceite formal. A declaração vai sempre `true`: o botão só habilita
/// depois que o Gerente marca a caixa.
Future<void> registrarAceite(WidgetRef ref, int riscoId,
    {required String justificativa, required String? condicoes}) async {
  await ref.read(apiProvider).post('/api/aceites/riscos/$riscoId', {
    'justificativa': justificativa,
    'condicoes': condicoes,
    'declaracao': true,
  });
  ref.invalidate(pendenciasProvider);
  ref.invalidate(riscosProvider);
  ref.invalidate(planosProvider);
  ref.invalidate(planoDoRiscoProvider(riscoId));
}
