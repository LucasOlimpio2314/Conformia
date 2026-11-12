import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import '../avisos/aviso_resumo.dart';

/// Mostra as notificações do Android (sem Firebase: o próprio app consulta a API).
///
/// O id da notificação é o id do aviso. Assim, ao ler um aviso no app, dá para
/// tirar exatamente aquela notificação da barra.
abstract final class Notificador {
  static final _plugin = FlutterLocalNotificationsPlugin();
  static bool _iniciado = false;

  /// O que fazer quando o usuário toca numa notificação com o app aberto.
  static void Function()? aoTocar;

  static const _canal = AndroidNotificationDetails(
    'avisos',
    'Avisos do Conformia',
    channelDescription: 'Aceites que caíram ou venceram e riscos prontos para aceite',
    importance: Importance.high,
    priority: Priority.high,
  );

  static Future<void> iniciar() async {
    if (_iniciado) return;
    await _plugin.initialize(
      settings: const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')),
      onDidReceiveNotificationResponse: (_) => aoTocar?.call(),
    );
    _iniciado = true;
  }

  /// Android 13+ pergunta ao usuário se o app pode notificar.
  static Future<bool> pedirPermissao() async {
    final android = _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    return await android?.requestNotificationsPermission() ?? false;
  }

  static Future<bool> permitido() async {
    final android = _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    return await android?.areNotificationsEnabled() ?? false;
  }

  /// O app foi aberto tocando numa notificação? (com o app fechado, o [aoTocar] não dispara)
  static Future<bool> abriuPorNotificacao() async {
    final detalhes = await _plugin.getNotificationAppLaunchDetails();
    return detalhes?.didNotificationLaunchApp ?? false;
  }

  static Future<void> mostrar(AvisoResumo aviso) => _plugin.show(
        id: aviso.id,
        title: aviso.titulo,
        body: aviso.mensagem,
        notificationDetails: const NotificationDetails(android: _canal),
        payload: aviso.link,
      );

  /// Muitos avisos de uma vez viram uma notificação só.
  static Future<void> mostrarResumo(int quantidade) => _plugin.show(
        id: 0,
        title: '$quantidade avisos novos',
        body: 'Toque para ver os avisos do Conformia',
        notificationDetails: const NotificationDetails(android: _canal),
        payload: '/avisos',
      );

  static Future<void> remover(int avisoId) => _plugin.cancel(id: avisoId);

  static Future<void> removerTodas() => _plugin.cancelAll();
}
