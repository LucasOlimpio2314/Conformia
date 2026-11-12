import 'package:conformia/avisos/aviso_resumo.dart';
import 'package:flutter_test/flutter_test.dart';

AvisoResumo _aviso(int id, {bool lido = false}) => AvisoResumo.deJson({
      'id': id,
      'tipo': 'RESIDUAL_PRONTO',
      'titulo': 'Residual pronto para aceite',
      'mensagem': 'RSK-001 concluiu o plano de ação.',
      'link': '/aceites',
      'criadoEm': '2026-11-11T11:12:00Z',
      'lido': lido,
    });

void main() {
  test('deveNotificarSoAvisosNovosQueAindaNaoForamNotificados', () {
    // Arrange
    final avisos = [_aviso(5), _aviso(3), _aviso(4)];

    // Act
    final novos = avisosParaNotificar(avisos, 3);

    // Assert
    expect(novos.map((a) => a.id), [4, 5]);
  });

  test('naoDeveNotificarAvisoJaLido', () {
    // Arrange
    final avisos = [_aviso(8, lido: true), _aviso(9)];

    // Act
    final novos = avisosParaNotificar(avisos, 0);

    // Assert
    expect(novos.map((a) => a.id), [9]);
  });

  test('naoDeveRepetirNotificacaoNaSegundaVerificacao', () {
    // Arrange — a primeira verificação já notificou até o 9
    final avisos = [_aviso(7), _aviso(9)];

    // Act
    final novos = avisosParaNotificar(avisos, 9);

    // Assert
    expect(novos, isEmpty);
  });
}
