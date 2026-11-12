import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:path_provider/path_provider.dart';

import 'config.dart';

/// Erro devolvido pela API no formato `{"codigo", "mensagem", "campo"?, "campos"?}`.
/// A `mensagem` já vem pronta para mostrar na tela.
class ApiException implements Exception {
  ApiException(this.status, this.codigo, this.mensagem, {this.campos = const {}});

  final int status;
  final String codigo;
  final String mensagem;

  /// Erro por campo (`DADOS_INVALIDOS` ou `REGRA_NEGOCIO` com `campo`).
  final Map<String, String> campos;

  /// Sessão acabou (expirou, logout ou usuário desativado): voltar ao login.
  bool get semSessao => status == 401 && codigo != 'CREDENCIAIS_INVALIDAS' && codigo != 'SENHA_EXPIRADA';

  static ApiException de(DioException e) {
    final resposta = e.response;
    if (resposta == null) {
      return ApiException(0, 'SEM_CONEXAO',
          'Não foi possível falar com o servidor ($apiUrl). Confira se ele está ligado.');
    }
    final corpo = resposta.data;
    if (corpo is Map && corpo['mensagem'] is String) {
      final campos = <String, String>{};
      if (corpo['campos'] is Map) {
        (corpo['campos'] as Map).forEach((k, v) => campos['$k'] = '$v');
      }
      if (corpo['campo'] is String) {
        campos[corpo['campo'] as String] = corpo['mensagem'] as String;
      }
      return ApiException(resposta.statusCode ?? 0, '${corpo['codigo']}', corpo['mensagem'] as String,
          campos: campos);
    }
    return ApiException(resposta.statusCode ?? 0, 'ERRO', 'Erro inesperado (${resposta.statusCode}).');
  }

  @override
  String toString() => mensagem;
}

/// Cliente HTTP do app. O login é por sessão, igual à web: o servidor manda o
/// cookie `JSESSIONID` e o [PersistCookieJar] guarda em arquivo, para:
/// - não pedir login de novo ao reabrir o app (enquanto a sessão não expirar);
/// - a tarefa de segundo plano (outro isolate) usar a mesma sessão.
class ApiClient {
  ApiClient._(this._dio, this._cookies);

  final Dio _dio;
  final PersistCookieJar _cookies;

  /// Chamado quando qualquer requisição volta 401 (sessão acabou).
  void Function(ApiException erro)? aoPerderSessao;

  static Future<ApiClient> criar() async {
    final pasta = await getApplicationSupportDirectory();
    final cookies = PersistCookieJar(storage: FileStorage('${pasta.path}/cookies/'));
    final dio = Dio(BaseOptions(
      baseUrl: apiUrl,
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 15),
      contentType: Headers.jsonContentType,
      headers: {'Accept': 'application/json'},
    ));
    dio.interceptors.add(CookieManager(cookies));
    return ApiClient._(dio, cookies);
  }

  Future<dynamic> get(String caminho) => _executar(() => _dio.get(caminho));

  Future<dynamic> post(String caminho, [Object? corpo]) => _executar(() => _dio.post(caminho, data: corpo));

  Future<dynamic> put(String caminho, Object? corpo) => _executar(() => _dio.put(caminho, data: corpo));

  /// Apaga o cookie da sessão (usado no logout).
  Future<void> limparSessao() => _cookies.deleteAll();

  Future<dynamic> _executar(Future<Response<dynamic>> Function() chamada) async {
    try {
      final resposta = await chamada();
      return resposta.data;
    } on DioException catch (e) {
      final erro = ApiException.de(e);
      if (erro.semSessao) {
        aoPerderSessao?.call(erro);
      }
      throw erro;
    }
  }
}
