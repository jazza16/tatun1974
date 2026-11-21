import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/officer_session.dart';

/// Kartu UI "Keamanan Perangkat & Sesi" yang mengambil data dari `officer_sessions`
/// (difilter berdasarkan NIP petugas yang sedang login) dengan teks verdict
/// "Akses Sah (Terverifikasi Server)" dan tombol merah "Cabut Akses".
class DeviceSecuritySessionCard extends StatelessWidget {
  final String loggedInNip;
  final VoidCallback onForceLogout;

  const DeviceSecuritySessionCard({
    super.key,
    required this.loggedInNip,
    required this.onForceLogout,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormatter = DateFormat('dd MMM yyyy, HH:mm', 'id_ID');

    return StreamBuilder<OfficerSession?>(
      stream: SessionManagementService.instance.watchSessionByNip(loggedInNip),
      builder: (context, snapshot) {
        final session = snapshot.data;

        // Jika sesi dicabut (is_active == false), jalankan logout paksa
        if (session != null && !session.isActive) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            onForceLogout();
          });
        }

        return Card(
          color: const Color(0xFF0F172A),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: const BorderSide(color: Color(0xFF1E293B), width: 1.2),
          ),
          elevation: 3,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: const Color(0xFF16A34A).withValues(alpha: 0.18),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(
                        Icons.verified_user_rounded,
                        color: Color(0xFF4ADE80),
                        size: 22,
                      ),
                    ),
                    const SizedBox(width: 12),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Keamanan Perangkat & Sesi',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                            ),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Manajemen Sesi Aktif Petugas (officer_sessions)',
                            style: TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: (session?.isActive ?? false)
                            ? const Color(0xFF166534)
                            : const Color(0xFF991B1B),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        (session?.isActive ?? false) ? 'AKTIF' : 'NONAKTIF',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
                const Divider(color: Color(0xFF1E293B), height: 24),
                if (session == null)
                  const Text(
                    'Tidak ada sesi aktif untuk NIP ini.',
                    style: TextStyle(color: Color(0xFF94A3B8), fontSize: 13),
                  )
                else ...[
                  _buildInfoRow('NIP Petugas', session.nip),
                  _buildInfoRow(
                    'Nama & Jabatan',
                    '${session.officerName} (${session.officerRole})',
                  ),
                  _buildInfoRow('Perangkat', session.deviceName),
                  _buildInfoRow('Versi OS', session.osVersion),
                  _buildInfoRow(
                    'Waktu Login',
                    '${dateFormatter.format(session.loginTime)} WIB',
                  ),
                  _buildInfoRow(
                    'Status Verdict',
                    session.verdict,
                    valueColor: const Color(0xFF4ADE80),
                  ),
                  const SizedBox(height: 14),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFDC2626),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                      onPressed: () async {
                        await SessionManagementService.instance
                            .revokeSession(session.nip);
                        onForceLogout();
                      },
                      icon: const Icon(Icons.gpp_bad_rounded, size: 18),
                      label: const Text(
                        'Cabut Akses',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildInfoRow(
    String label,
    String value, {
    Color valueColor = Colors.white,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: const TextStyle(
                color: Color(0xFF94A3B8),
                fontSize: 12.5,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                color: valueColor,
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
