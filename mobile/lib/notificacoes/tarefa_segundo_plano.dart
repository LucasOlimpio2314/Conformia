import 'package:flutter/widgets.dart';
import 'package:workmanager/workmanager.dart';

import '../core/api_client.dart';
import 'verificador_avisos.dart';

/// Ponto de entrada da tarefa periódica. O Android chama esta função num
/// isolate separado, mesmo com o app fechado — por isso ela cria o próprio
/// [ApiClient] (que lê o cookie da sessão salvo em arquivo).
///
/// `vm:entry-point` impede o compilador de remover a função, já que nenhum
/// código Dart a chama diretamente.
@pragma('vm:entry-point')
void tarefaSegundoPlano() {
  Workmanager().executeTask((tarefa, dados) async {
    WidgetsFlutterBinding.ensureInitialized();
    try {
      final api = await ApiClient.criar();
      await VerificadorAvisos.verificar(api);
    } on ApiException catch (e) {
      // Sessão expirou ou servidor desligado: não há o que notificar.
      // Devolve true para o Android não ficar repetindo a tarefa.
      debugPrint('Conformia: verificação em segundo plano sem sucesso (${e.codigo})');
    }
    return true;
  });
}
