import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';

import '../core/config.dart';
import 'notificador.dart';
import 'verificador_avisos.dart';

/// Liga e desliga a consulta de avisos em segundo plano (app fechado).
abstract final class AgendadorAvisos {
  static const nomeTarefa = 'conformia.verificar-avisos';
  static const _chaveUsuario = 'avisos.usuarioId';

  /// Chamado depois do login de um Gerente.
  static Future<void> iniciar(int usuarioId) async {
    // Outro usuário entrou neste celular: começa a contagem do zero.
    final prefs = SharedPreferencesAsync();
    if (await prefs.getInt(_chaveUsuario) != usuarioId) {
      await VerificadorAvisos.esquecer();
      await prefs.setInt(_chaveUsuario, usuarioId);
    }
    await Notificador.iniciar();
    if (!await Notificador.permitido()) {
      await Notificador.pedirPermissao();
    }
    await Workmanager().registerPeriodicTask(
      nomeTarefa,
      nomeTarefa,
      frequency: intervaloAvisosSegundoPlano,
      constraints: Constraints(networkType: NetworkType.connected),
      // Se já estiver agendada (ex.: reabriu o app), mantém a que existe.
      existingWorkPolicy: ExistingPeriodicWorkPolicy.keep,
    );
  }

  /// Logout, sessão expirada ou usuário que não é Gerente.
  static Future<void> parar() async {
    await Workmanager().cancelByUniqueName(nomeTarefa);
    await VerificadorAvisos.esquecer();
    await SharedPreferencesAsync().remove(_chaveUsuario);
    await Notificador.iniciar();
    await Notificador.removerTodas();
  }
}
