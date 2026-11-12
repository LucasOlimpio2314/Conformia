/// Endereço do servidor Spring.
///
/// No emulador, `10.0.2.2` é o "localhost" do PC. Para um celular de verdade na
/// mesma rede, rode com: `flutter run --dart-define=API_URL=http://192.168.0.10:8080`
const String apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://10.0.2.2:8080');

/// Com o app aberto, consulta os avisos a cada 1 minuto.
const Duration intervaloAvisosAberto = Duration(minutes: 1);

/// Com o app fechado o Android não deixa menos que 15 minutos (regra do WorkManager).
const Duration intervaloAvisosSegundoPlano = Duration(minutes: 15);
