/// Sistem Role-Based Access Control (RBAC) Multi-Tier BPBD Kabupaten Mukomuko.
/// Menggantikan sistem SuperAdmin/Operator lama dengan 3 Tier jabatan resmi.
enum UserRoleTier {
  tier1FullAccess, // Khusus Manajer
  tier2HighAccess, // Pengawas, Supervisor, Koord. Administrasi
  tier3StandardAccess, // Operator, Logistik, TRC, atau null/kosong
}

class UserRole {
  final String rawRole;
  final UserRoleTier tier;

  const UserRole._(this.rawRole, this.tier);

  factory UserRole.fromString(String? role) {
    final cleaned = (role ?? '').trim();
    if (isManajer(cleaned)) {
      return UserRole._(cleaned, UserRoleTier.tier1FullAccess);
    } else if (isHighAccess(cleaned)) {
      return UserRole._(cleaned, UserRoleTier.tier2HighAccess);
    } else {
      return UserRole._(
        cleaned.isEmpty ? 'Operator' : cleaned,
        UserRoleTier.tier3StandardAccess,
      );
    }
  }

  /// Tier 1: Full Access (Khusus Manajer)
  static bool isManajer(String? role) => (role ?? '').trim() == 'Manajer';

  /// Tier 2: High Access (Pengawas & Koordinator)
  static bool isHighAccess(String? role) {
    final r = (role ?? '').trim();
    return r == 'Pengawas' || r == 'Supervisor' || r == 'Koord. Administrasi';
  }

  /// Tier 3: Standard Access (Operator, Logistik, TRC, atau null)
  static bool isStandardAccess(String? role) {
    return !isManajer(role) && !isHighAccess(role);
  }

  // Hak akses spesifik komponen UI:
  static bool canManagePersonnel(String? role) => isManajer(role);
  static bool canAccessSystemSettings(String? role) => isManajer(role);
  static bool canDeleteReport(String? role) => isManajer(role);
  static bool canValidateReport(String? role) =>
      isManajer(role) || isHighAccess(role);
  static bool canViewStatistics(String? role) =>
      isManajer(role) || isHighAccess(role);
}
