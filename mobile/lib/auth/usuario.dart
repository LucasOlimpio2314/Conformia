/// Espelho do `UsuarioResposta` da API (docs/api/usuarios.md).
class Usuario {
  const Usuario({
    required this.id,
    required this.nome,
    required this.email,
    required this.perfil,
    required this.situacao,
    this.ultimoAcesso,
  });

  final int id;
  final String nome;
  final String email;

  /// GERENTE, ANALISTA ou AUDITOR.
  final String perfil;
  final String situacao;
  final DateTime? ultimoAcesso;

  bool get gerente => perfil == 'GERENTE';
  bool get auditor => perfil == 'AUDITOR';

  /// Gerente e Analista alteram; Auditor só lê.
  bool get podeAlterar => !auditor;

  String get perfilRotulo => switch (perfil) {
        'GERENTE' => 'Gerente',
        'ANALISTA' => 'Analista',
        'AUDITOR' => 'Auditor',
        _ => perfil,
      };

  String get iniciais {
    final partes = nome.trim().split(RegExp(r'\s+'));
    final primeira = partes.first.isEmpty ? '?' : partes.first[0];
    final ultima = partes.length > 1 ? partes.last[0] : '';
    return (primeira + ultima).toUpperCase();
  }

  factory Usuario.deJson(Map<String, dynamic> j) => Usuario(
        id: (j['id'] as num).toInt(),
        nome: j['nome'] as String,
        email: j['email'] as String,
        perfil: j['perfil'] as String,
        situacao: j['situacao'] as String? ?? 'ATIVO',
        ultimoAcesso: j['ultimoAcesso'] == null ? null : DateTime.parse(j['ultimoAcesso'] as String),
      );
}

/// Espelho do `SessaoResposta`.
class Sessao {
  const Sessao(this.usuario, this.trocaSenhaObrigatoria);

  final Usuario usuario;
  final bool trocaSenhaObrigatoria;

  factory Sessao.deJson(Map<String, dynamic> j) =>
      Sessao(Usuario.deJson(j['usuario'] as Map<String, dynamic>), j['trocaSenhaObrigatoria'] == true);
}
