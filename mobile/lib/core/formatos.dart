// Datas no formato brasileiro, sem depender do pacote intl.

String _dois(int n) => n.toString().padLeft(2, '0');

/// `2026-11-08` → `08/11/2026`
String dataBr(DateTime d) => '${_dois(d.day)}/${_dois(d.month)}/${d.year}';

/// `2026-11-08` → `08/11`
String diaMes(DateTime d) => '${_dois(d.day)}/${_dois(d.month)}';

/// Instante UTC da API → horário local, ex.: `hoje 08:12`, `ontem 17:40`, `08/11 06:00`.
String quando(DateTime instante, {DateTime? agora}) {
  final local = instante.toLocal();
  final ref = (agora ?? DateTime.now()).toLocal();
  final hora = '${_dois(local.hour)}:${_dois(local.minute)}';
  final dia = DateTime(local.year, local.month, local.day);
  final hoje = DateTime(ref.year, ref.month, ref.day);
  final diferenca = hoje.difference(dia).inDays;
  if (diferenca == 0) return 'hoje $hora';
  if (diferenca == 1) return 'ontem $hora';
  return '${diaMes(local)} $hora';
}

/// Data sem hora no formato da API (`yyyy-MM-dd`).
String dataApi(DateTime d) => '${d.year}-${_dois(d.month)}-${_dois(d.day)}';
