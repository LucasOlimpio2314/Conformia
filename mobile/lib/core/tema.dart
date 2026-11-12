import 'package:material_ui/material_ui.dart';

/// Cores da versão web (static/css/app.css) para o app ter a mesma cara.
abstract final class Cores {
  static const indigo = Color(0xFF4338CA);
  static const indigo50 = Color(0xFFEEF0FB);
  static const indigo100 = Color(0xFFDFE2F8);
  static const ink = Color(0xFF1B1D2E);
  static const ink2 = Color(0xFF4A4E66);
  static const ink3 = Color(0xFF7A7E96);
  static const linha = Color(0xFFE3E5EF);
  static const linha2 = Color(0xFFEEF0F6);
  static const fundo = Color(0xFFF6F7FB);
  static const superficie = Color(0xFFFFFFFF);
  static const ok = Color(0xFF157A4B);
  static const okFundo = Color(0xFFE3F3EA);
  static const alerta = Color(0xFF9A5B00);
  static const alertaFundo = Color(0xFFFDF1DD);
  static const erro = Color(0xFFB42318);
  static const erroFundo = Color(0xFFFDECEA);
}

ThemeData temaConformia() {
  const borda = OutlineInputBorder(
    borderRadius: BorderRadius.all(Radius.circular(8)),
    borderSide: BorderSide(color: Cores.linha),
  );
  return ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(
      seedColor: Cores.indigo,
      primary: Cores.indigo,
      surface: Cores.superficie,
      error: Cores.erro,
    ),
    scaffoldBackgroundColor: Cores.fundo,
    appBarTheme: const AppBarTheme(
      backgroundColor: Cores.superficie,
      foregroundColor: Cores.ink,
      elevation: 0,
      scrolledUnderElevation: 0,
      titleTextStyle: TextStyle(color: Cores.ink, fontSize: 18, fontWeight: FontWeight.w600),
      shape: Border(bottom: BorderSide(color: Cores.linha)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Cores.superficie,
      border: borda,
      enabledBorder: borda,
      focusedBorder: borda.copyWith(borderSide: const BorderSide(color: Cores.indigo, width: 2)),
      errorBorder: borda.copyWith(borderSide: const BorderSide(color: Cores.erro)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      // Mensagens do servidor podem ser longas ("entre 20 e 1000 caracteres").
      errorMaxLines: 3,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(46),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(46),
        foregroundColor: Cores.ink,
        side: const BorderSide(color: Cores.linha),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    ),
    cardTheme: const CardThemeData(
      color: Cores.superficie,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(8)),
        side: BorderSide(color: Cores.linha),
      ),
    ),
    navigationBarTheme: const NavigationBarThemeData(
      backgroundColor: Cores.superficie,
      indicatorColor: Cores.indigo50,
      height: 64,
    ),
  );
}
