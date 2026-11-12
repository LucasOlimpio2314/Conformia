import 'package:shared_preferences/shared_preferences.dart';

import '../avisos/aviso_resumo.dart';
import '../core/api_client.dart';
import 'notificador.dart';

/// Consulta `/api/avisos` e notifica só o que é novo.
///
/// Roda em dois lugares:
/// - com o app aberto, a cada 1 minuto (Timer na tela principal);
/// - com o app fechado, a cada 15 minutos (WorkManager, em outro isolate).
///
/// Por isso a "memória" do último aviso notificado fica no disco
/// ([SharedPreferencesAsync] lê direto do disco, sem cache, e os dois lados veem o mesmo valor).
abstract final class VerificadorAvisos {
  static const chaveUltimo = 'avisos.ultimoNotificado';

  /// Devolve a lista de avisos (para a tela usar) ou lança [ApiException].
  static Future<List<AvisoResumo>> verificar(ApiClient api) async {
    final json = await api.get('/api/avisos') as List;
    final avisos = [for (final a in json) AvisoResumo.deJson(a as Map<String, dynamic>)];

    final prefs = SharedPreferencesAsync();
    final ultimo = await prefs.getInt(chaveUltimo) ?? 0;
    final novos = avisosParaNotificar(avisos, ultimo);
    if (novos.isEmpty) return avisos;

    // Grava ANTES de mostrar: se as duas verificações rodarem juntas, a segunda já
    // encontra o número atualizado e não repete a notificação.
    await prefs.setInt(chaveUltimo, novos.last.id);

    await Notificador.iniciar();
    if (novos.length > 3) {
      await Notificador.mostrarResumo(novos.length);
    } else {
      for (final aviso in novos) {
        await Notificador.mostrar(aviso);
      }
    }
    return avisos;
  }

  /// Ao sair (ou trocar de usuário) esquece o que já foi notificado.
  static Future<void> esquecer() => SharedPreferencesAsync().remove(chaveUltimo);
}
