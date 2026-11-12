import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/providers.dart';
import '../notificacoes/notificador.dart';
import '../notificacoes/verificador_avisos.dart';
import 'aviso_resumo.dart';

/// Lista de avisos. Usa o mesmo [VerificadorAvisos] da notificação: abrir a
/// lista também conta como "já vi" para fins de notificação.
final avisosProvider = FutureProvider.autoDispose<List<AvisoResumo>>((ref) {
  return VerificadorAvisos.verificar(ref.read(apiProvider));
});

/// Número do sininho.
final naoLidosProvider = FutureProvider.autoDispose<int>((ref) async {
  final json = await ref.read(apiProvider).get('/api/avisos/nao-lidos') as Map<String, dynamic>;
  return (json['quantidade'] as num).toInt();
});

/// Marca como lido e tira a notificação dele da barra do Android.
Future<void> marcarLido(WidgetRef ref, AvisoResumo aviso) async {
  if (!aviso.lido) {
    await ref.read(apiProvider).post('/api/avisos/${aviso.id}/lido');
  }
  await Notificador.remover(aviso.id);
  ref.invalidate(avisosProvider);
  ref.invalidate(naoLidosProvider);
}

Future<void> marcarTodosLidos(WidgetRef ref) async {
  await ref.read(apiProvider).post('/api/avisos/lidos');
  await Notificador.removerTodas();
  ref.invalidate(avisosProvider);
  ref.invalidate(naoLidosProvider);
}
