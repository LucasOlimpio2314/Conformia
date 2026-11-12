import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/providers.dart';
import '../riscos/riscos_providers.dart';
import 'plano.dart';

/// Todos os planos de ação (`GET /api/plano`). A tela filtra as ações do usuário logado.
/// Não existe um endpoint "minhas ações": o filtro por responsável é feito no app.
final planosProvider = FutureProvider.autoDispose<List<PlanoRisco>>((ref) async {
  final json = await ref.read(apiProvider).get('/api/plano') as List;
  return [for (final p in json) PlanoRisco.deJson(p as Map<String, dynamic>)];
});

/// Atualiza a situação e a evidência de uma ação. Depois recarrega os planos.
Future<void> salvarAcao(WidgetRef ref, Acao acao, {required String situacao, required String? evidencia}) async {
  await ref.read(apiProvider).put('/api/plano/acoes/${acao.id}', acao.paraForm(situacao: situacao, evidencia: evidencia));
  ref.invalidate(planosProvider);
  ref.invalidate(planoDoRiscoProvider(acao.riscoId));
}
