import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_client.dart';

/// O [ApiClient] precisa de `await` para abrir o arquivo de cookies, então ele é
/// criado no `main()` e entra aqui com `overrideWithValue`.
final apiProvider = Provider<ApiClient>((ref) => throw UnimplementedError('apiProvider sem override'));

/// Aba selecionada na barra de baixo. Fica num provider para o aviso (ou a
/// notificação) conseguir abrir a aba Aceites de qualquer lugar.
final abaProvider = NotifierProvider<AbaController, String>(AbaController.new);

class AbaController extends Notifier<String> {
  @override
  String build() => 'acoes';

  void abrir(String aba) => state = aba;
}
