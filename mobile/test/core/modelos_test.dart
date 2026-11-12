import 'package:conformia/auth/regras_senha.dart';
import 'package:conformia/auth/usuario.dart';
import 'package:conformia/core/formatos.dart';
import 'package:conformia/riscos/risco.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('deveLerSessaoComTrocaDeSenhaObrigatoria', () {
    // Arrange
    final json = {
      'usuario': {
        'id': 1,
        'nome': 'Ana Souza',
        'email': 'ana@empresa.com.br',
        'perfil': 'GERENTE',
        'ativo': true,
        'situacao': 'SENHA_REDEFINIDA',
        'ultimoAcesso': null,
        'criadoEm': '2026-11-11T11:00:00Z',
      },
      'trocaSenhaObrigatoria': true,
    };

    // Act
    final sessao = Sessao.deJson(json);

    // Assert
    expect(sessao.trocaSenhaObrigatoria, isTrue);
    expect(sessao.usuario.gerente, isTrue);
    expect(sessao.usuario.iniciais, 'AS');
  });

  test('deveImpedirAuditorDeAlterar', () {
    // Arrange
    const auditor = Usuario(id: 3, nome: 'Rui', email: 'rui@x.com', perfil: 'AUDITOR', situacao: 'ATIVO');

    // Act
    final pode = auditor.podeAlterar;

    // Assert
    expect(pode, isFalse);
  });

  test('deveRecusarSenhaSemNumero', () {
    // Arrange
    const regras = RegrasSenha('SomenteLetras');

    // Act
    final valida = regras.valida;

    // Assert
    expect(valida, isFalse);
  });

  test('deveAceitarSenhaComLetrasNumerosEOitoCaracteres', () {
    // Arrange
    const regras = RegrasSenha('Conforme1');

    // Act
    final valida = regras.valida;

    // Assert
    expect(valida, isTrue);
  });

  test('deveEncontrarRiscoPeloNomeDoAtivo', () {
    // Arrange
    final risco = Risco.deJson({
      'id': 1,
      'codigo': 'RSK-001',
      'titulo': 'Ransomware nos servidores',
      'ameaca': {'id': 1, 'nome': 'Ransomware / malware'},
      'vulnerabilidade': {'id': 2, 'nome': 'Backup sem cópia isolada'},
      'ativos': [
        {'id': 3, 'codigo': 'ATV-003', 'nome': 'ERP', 'emUso': true, 'criticidade': 5},
      ],
      'controles': [
        {'codigo': '8.13', 'nome': 'Backup de informações', 'tema': 8},
      ],
      'donoNome': 'Mariana',
      'probabilidade': 4,
      'impacto': 5,
      'valor': 20,
      'nivel': 'CRITICO',
      'tratamento': null,
      'situacao': 'ATIVO',
    });

    // Act
    final achou = risco.combina('erp');

    // Assert
    expect(achou, isTrue);
    expect(risco.temPlano, isFalse);
    expect(risco.tratamentoRotulo, 'Tratamento a definir');
  });

  test('deveMostrarHojeParaAvisoDoMesmoDia', () {
    // Arrange
    final instante = DateTime(2026, 11, 11, 8, 12);

    // Act
    final texto = quando(instante, agora: DateTime(2026, 11, 11, 20, 0));

    // Assert
    expect(texto, 'hoje 08:12');
  });
}
