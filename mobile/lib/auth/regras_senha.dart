/// Mesmas regras de nova senha do `UsuarioService` no servidor:
/// 8 a 64 caracteres, com letras e números. Aqui é só para mostrar ✓ ao digitar;
/// quem decide de verdade é o servidor.
class RegrasSenha {
  const RegrasSenha(this.senha);

  final String senha;

  bool get tamanho => senha.length >= 8 && senha.length <= 64;
  bool get letra => RegExp(r'[A-Za-zÀ-ÿ]').hasMatch(senha);
  bool get numero => RegExp(r'\d').hasMatch(senha);

  bool get valida => tamanho && letra && numero;
}
