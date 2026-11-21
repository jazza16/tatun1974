import 'dart:async';

/// Model dokumen koleksi `officer_sessions`
/// Field wajib: nip, device_name, os_version, login_time, is_active
class OfficerSession {
  final String sessionId;
  final String nip;
  final String officerName;
  final String officerRole;
  final String deviceName;
  final String osVersion;
  final DateTime loginTime;
  final bool isActive;
  final String verdict;

  const OfficerSession({
    required this.sessionId,
    required this.nip,
    required this.officerName,
    required this.officerRole,
    required this.deviceName,
    required this.osVersion,
    required this.loginTime,
    this.isActive = true,
    this.verdict = 'Akses Sah (Terverifikasi Server)',
  });

  OfficerSession copyWith({
    bool? isActive,
    String? verdict,
  }) {
    return OfficerSession(
      sessionId: sessionId,
      nip: nip,
      officerName: officerName,
      officerRole: officerRole,
      deviceName: deviceName,
      osVersion: osVersion,
      loginTime: loginTime,
      isActive: isActive ?? this.isActive,
      verdict: verdict ?? this.verdict,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'nip': nip,
      'device_name': deviceName,
      'os_version': osVersion,
      'login_time': loginTime.millisecondsSinceEpoch,
      'is_active': isActive,
      'verdict': verdict,
    };
  }
}

/// Service Manajemen Sesi (`officer_sessions`) pengganti Play Integrity API.
class SessionManagementService {
  SessionManagementService._();
  static final SessionManagementService instance = SessionManagementService._();

  final Map<String, OfficerSession> _officerSessions = {};
  final StreamController<Map<String, OfficerSession>> _controller =
      StreamController<Map<String, OfficerSession>>.broadcast();

  /// Saat petugas berhasil login, buat dokumen sesi baru di `officer_sessions`
  /// dengan `is_active: true` dan verdict `"Akses Sah (Terverifikasi Server)"`.
  Future<OfficerSession> createSessionOnLogin({
    required String nip,
    required String officerName,
    required String officerRole,
    String deviceName = 'Android Device BPBD',
    String osVersion = 'Android 14 (API 34)',
  }) async {
    final cleanNip = nip.trim();
    final session = OfficerSession(
      sessionId: 'sess_${DateTime.now().millisecondsSinceEpoch}_$cleanNip',
      nip: cleanNip,
      officerName: officerName,
      officerRole: officerRole,
      deviceName: deviceName,
      osVersion: osVersion,
      loginTime: DateTime.now(),
      isActive: true,
      verdict: 'Akses Sah (Terverifikasi Server)',
    );
    _officerSessions[cleanNip] = session;
    _controller.add(Map.unmodifiable(_officerSessions));
    return session;
  }

  /// Stream sesi berdasarkan NIP petugas yang sedang login.
  Stream<OfficerSession?> watchSessionByNip(String nip) async* {
    final cleanNip = nip.trim();
    yield _officerSessions[cleanNip];
    await for (final map in _controller.stream) {
      yield map[cleanNip];
    }
  }

  /// Tombol merah "Cabut Akses": mengubah `is_active` menjadi `false`,
  /// menghapus sesi aktif, dan memicu aplikasi petugas untuk logout paksa.
  Future<void> revokeSession(String nip) async {
    final cleanNip = nip.trim();
    final existing = _officerSessions[cleanNip];
    if (existing != null) {
      _officerSessions[cleanNip] = existing.copyWith(isActive: false);
      _controller.add(Map.unmodifiable(_officerSessions));
      _officerSessions.remove(cleanNip);
      _controller.add(Map.unmodifiable(_officerSessions));
    }
  }
}
