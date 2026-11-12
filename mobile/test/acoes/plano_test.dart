import 'package:conformia/acoes/plano.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> _acaoJson({
  required int id,
  required int responsavelId,
  required String prazo,
  String situacao = 'PENDENTE',
  bool atrasada = false,
}) =>
    {
      'id': id,
      'codigo': 'ACT-${id.toString().padLeft(3, '0')}',
      'riscoId': 1,
      'titulo': 'Ação $id',
      'responsavelId': responsavelId,
      'responsavelNome': 'Fulano',
      'responsavelAtivo': true,
      'prazo': prazo,
      'controle': {'codigo': '8.13', 'nome': 'Backup de informações', 'tema': 8},
      'situacao': situacao,
      'evidencia': null,
      'atrasada': atrasada,
    };

PlanoRisco _plano(List<Map<String, dynamic>> acoes) => PlanoRisco.deJson({
      'riscoId': 1,
      'codigo': 'RSK-001',
      'titulo': 'Perda de dados do ERP',
      'tratamento': 'MITIGAR',
      'probabilidade': 3,
      'impacto': 4,
      'valor': 12,
      'nivel': 'ALTO',
      'probabilidadeResidual': null,
      'impactoResidual': null,
      'valorResidual': null,
      'nivelResidual': null,
      'acoes': acoes,
      'concluidas': 0,
      'validas': acoes.length,
      'prontoParaAceite': false,
      'aceiteVigente': null,
    });

void main() {
  test('deveListarSoAsAcoesDoResponsavelLogado', () {
    // Arrange
    final planos = [
      _plano([
        _acaoJson(id: 1, responsavelId: 7, prazo: '2026-12-01'),
        _acaoJson(id: 2, responsavelId: 9, prazo: '2026-12-01'),
      ]),
    ];

    // Act
    final minhas = minhasAcoes(planos, 7);

    // Assert
    expect(minhas.map((m) => m.acao.codigo), ['ACT-001']);
  });

  test('deveMostrarAtrasadasPrimeiroEDepoisPorPrazo', () {
    // Arrange
    final planos = [
      _plano([
        _acaoJson(id: 1, responsavelId: 7, prazo: '2026-12-20'),
        _acaoJson(id: 2, responsavelId: 7, prazo: '2026-12-05'),
        _acaoJson(id: 3, responsavelId: 7, prazo: '2026-11-08', atrasada: true),
      ]),
    ];

    // Act
    final minhas = minhasAcoes(planos, 7);

    // Assert
    expect(minhas.map((m) => m.acao.id), [3, 2, 1]);
  });

  test('deveMandarOFormularioInteiroAoAtualizarSituacao', () {
    // Arrange
    final acao = Acao.deJson(_acaoJson(id: 4, responsavelId: 7, prazo: '2026-11-08'));

    // Act
    final form = acao.paraForm(situacao: 'CONCLUIDA', evidencia: 'Log do job anexado');

    // Assert
    expect(form, {
      'titulo': 'Ação 4',
      'responsavelId': 7,
      'prazo': '2026-11-08',
      'controle': '8.13',
      'situacao': 'CONCLUIDA',
      'evidencia': 'Log do job anexado',
    });
  });

  test('deveConsiderarConcluidaComoNaoEditavel', () {
    // Arrange
    final acao = Acao.deJson(_acaoJson(id: 5, responsavelId: 7, prazo: '2026-11-08', situacao: 'CONCLUIDA'));

    // Act
    final aberta = acao.aberta;

    // Assert
    expect(aberta, isFalse);
  });

  test('deveContarDiasDeAtrasoPeloPrazo', () {
    // Arrange
    final acao = Acao.deJson(_acaoJson(id: 6, responsavelId: 7, prazo: '2026-11-08', atrasada: true));

    // Act
    final dias = acao.diasDeAtraso(DateTime(2026, 11, 11, 15, 30));

    // Assert
    expect(dias, 3);
  });
}
