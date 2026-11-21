import 'package:flutter/material.dart';
import '../models/officer_session.dart';
import '../models/user_role.dart';
import '../widgets/device_security_session_card.dart';
import '../widgets/karhutla_chart_card.dart';

void main() {
  runApp(const BpbdMukomukoApp());
}

class BpbdMukomukoApp extends StatelessWidget {
  const BpbdMukomukoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Cuaca & Pusdalops BPBD Mukomuko',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF020617),
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF15803D),
          brightness: Brightness.dark,
        ),
      ),
      home: const LandingLoginScreen(),
    );
  }
}

/// 1. Halaman Landing & Login Petugas:
/// - Menggunakan BoxFit.cover & Alignment.topCenter pada latar belakang.
/// - TANPA tombol duplikat "SOP Evakuasi (100% Offline)" di Landing Page
///   (karena sudah tersedia di AppBar dalam aplikasi).
/// - Menggunakan SafeArea, resizeToAvoidBottomInset: true, SingleChildScrollView,
///   scrollPadding: EdgeInsets.only(bottom: 100), dan TextInputAction.next / done.
class LandingLoginScreen extends StatefulWidget {
  const LandingLoginScreen({super.key});

  @override
  State<LandingLoginScreen> createState() => _LandingLoginScreenState();
}

class _LandingLoginScreenState extends State<LandingLoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nipController = TextEditingController(text: '197804122005011004');
  final _nameController = TextEditingController(text: 'Ahmad Fauzi, S.T.');
  final _pinController = TextEditingController();
  String _selectedRole = 'Manajer';

  static const List<String> _availableRoles = [
    'Manajer',
    'Pengawas',
    'Supervisor',
    'Koord. Administrasi',
    'Operator',
    'Logistik',
    'TRC',
  ];

  @override
  void dispose() {
    _nipController.dispose();
    _nameController.dispose();
    _pinController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    FocusScope.of(context).unfocus();

    final nip = _nipController.text.trim();
    final name = _nameController.text.trim();
    final role = _selectedRole;

    // Buat dokumen sesi baru di koleksi officer_sessions
    await SessionManagementService.instance.createSessionOnLogin(
      nip: nip,
      officerName: name,
      officerRole: role,
    );

    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(
        builder: (_) => MainOperationalShell(
          officerNip: nip,
          officerName: name,
          officerRole: role,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bottomSafePadding = MediaQuery.of(context).viewPadding.bottom;

    return Scaffold(
      resizeToAvoidBottomInset: true,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Wallpaper Background dengan BoxFit.cover & Alignment.topCenter
          DecoratedBox(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Color(0xFF064E3B),
                  Color(0xFF0F172A),
                  Color(0xFF020617),
                ],
              ),
            ),
            child: Align(
              alignment: Alignment.topCenter,
              child: Opacity(
                opacity: 0.15,
                child: Icon(
                  Icons.shield_rounded,
                  size: MediaQuery.of(context).size.width * 0.85,
                  color: Colors.greenAccent,
                ),
              ),
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: EdgeInsets.fromLTRB(
                20,
                24,
                20,
                24 + bottomSafePadding,
              ),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const SizedBox(height: 16),
                    const Icon(
                      Icons.health_and_safety_rounded,
                      size: 64,
                      color: Color(0xFFF97316),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'PUSDALOPS-PB BPBD\nKABUPATEN MUKOMUKO',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: Colors.white,
                        height: 1.3,
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Sistem Informasi Cuaca, Pelaporan Warga & DALA',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 13,
                        color: Color(0xFF94A3B8),
                      ),
                    ),
                    const SizedBox(height: 28),
                    Card(
                      color: const Color(0xFF0F172A).withValues(alpha: 0.92),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                        side: const BorderSide(color: Color(0xFF1E293B)),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(18),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const Text(
                              'Login Ruang Kerja Petugas',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _nipController,
                              keyboardType: TextInputType.number,
                              textInputAction: TextInputAction.next,
                              scrollPadding: const EdgeInsets.only(bottom: 100),
                              decoration: const InputDecoration(
                                labelText: 'NIP Petugas',
                                prefixIcon: Icon(Icons.badge_outlined),
                                border: OutlineInputBorder(),
                              ),
                              validator: (val) =>
                                  (val == null || val.trim().isEmpty)
                                      ? 'NIP wajib diisi'
                                      : null,
                            ),
                            const SizedBox(height: 14),
                            TextFormField(
                              controller: _nameController,
                              textInputAction: TextInputAction.next,
                              scrollPadding: const EdgeInsets.only(bottom: 100),
                              decoration: const InputDecoration(
                                labelText: 'Nama Lengkap Petugas',
                                prefixIcon: Icon(Icons.person_outline),
                                border: OutlineInputBorder(),
                              ),
                              validator: (val) =>
                                  (val == null || val.trim().isEmpty)
                                      ? 'Nama petugas wajib diisi'
                                      : null,
                            ),
                            const SizedBox(height: 14),
                            DropdownButtonFormField<String>(
                              value: _selectedRole,
                              decoration: const InputDecoration(
                                labelText: 'Jabatan (Multi-Tier RBAC)',
                                prefixIcon:
                                    Icon(Icons.admin_panel_settings_outlined),
                                border: OutlineInputBorder(),
                              ),
                              items: _availableRoles
                                  .map(
                                    (role) => DropdownMenuItem(
                                      value: role,
                                      child: Text(role),
                                    ),
                                  )
                                  .toList(),
                              onChanged: (val) {
                                if (val != null) {
                                  setState(() => _selectedRole = val);
                                }
                              },
                            ),
                            const SizedBox(height: 14),
                            TextFormField(
                              controller: _pinController,
                              obscureText: true,
                              textInputAction: TextInputAction.done,
                              scrollPadding: const EdgeInsets.only(bottom: 100),
                              onFieldSubmitted: (_) => _handleLogin(),
                              decoration: const InputDecoration(
                                labelText: 'PIN Otorisasi Pusdalops',
                                prefixIcon: Icon(Icons.lock_outline),
                                border: OutlineInputBorder(),
                              ),
                              validator: (val) =>
                                  (val == null || val.trim().isEmpty)
                                      ? 'PIN wajib diisi'
                                      : null,
                            ),
                            const SizedBox(height: 20),
                            ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFF15803D),
                                foregroundColor: Colors.white,
                                padding:
                                    const EdgeInsets.symmetric(vertical: 14),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                              ),
                              onPressed: _handleLogin,
                              icon: const Icon(Icons.login_rounded),
                              label: const Text(
                                'MASUK RUANG KERJA',
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 2. Shell Utama Aplikasi dengan RBAC Multi-Tier BPBD Mukomuko, SafeArea,
///    Monitoring Karhutla, Form DALA, dan Kartu Keamanan Perangkat & Sesi.
class MainOperationalShell extends StatefulWidget {
  final String officerNip;
  final String officerName;
  final String officerRole;

  const MainOperationalShell({
    super.key,
    required this.officerNip,
    required this.officerName,
    required this.officerRole,
  });

  @override
  State<MainOperationalShell> createState() => _MainOperationalShellState();
}

class _MainOperationalShellState extends State<MainOperationalShell> {
  int _selectedIndex = 0;

  void _forceLogout() {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'Akses sesi perangkat telah dicabut. Logout paksa dijalankan.',
        ),
        backgroundColor: Color(0xFFDC2626),
      ),
    );
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LandingLoginScreen()),
      (route) => false,
    );
  }

  void _showOfflineSopDialog() {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('SOP Evakuasi (100% Offline)'),
        content: const Text(
          '1. Segera menuju titik kumpul evakuasi bukit/dataran tinggi terdekat.\n'
          '2. Ikuti arahan sirine EWS dan petugas TRC BPBD Kabupaten Mukomuko.\n'
          '3. Hubungi Call Center Pusdalops BPBD: (0737) 71113.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Tutup'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final role = widget.officerRole;
    final isTier1Manajer = UserRole.isManajer(role);
    final isTier2High = UserRole.isHighAccess(role);

    final pages = <Widget>[
      _buildDashboardTab(role, isTier1Manajer, isTier2High),
      const CitizenReportFormTab(),
      const DalaAssessmentFormTab(),
    ];

    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        backgroundColor: const Color(0xFF15803D),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'BPBD Kabupaten Mukomuko',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            Text(
              '${widget.officerName} • $role',
              style: const TextStyle(fontSize: 11.5, color: Colors.white70),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'SOP Evakuasi (100% Offline)',
            onPressed: _showOfflineSopDialog,
            icon: const Icon(Icons.menu_book_rounded),
          ),
          IconButton(
            tooltip: 'Keluar',
            onPressed: () async {
              await SessionManagementService.instance
                  .revokeSession(widget.officerNip);
              _forceLogout();
            },
            icon: const Icon(Icons.logout_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: pages[_selectedIndex],
      ),
      bottomNavigationBar: SafeArea(
        child: NavigationBar(
          selectedIndex: _selectedIndex,
          onDestinationSelected: (idx) => setState(() => _selectedIndex = idx),
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.dashboard_outlined),
              selectedIcon: Icon(Icons.dashboard_rounded),
              label: 'Dashboard',
            ),
            NavigationDestination(
              icon: Icon(Icons.campaign_outlined),
              selectedIcon: Icon(Icons.campaign_rounded),
              label: 'Laporan Warga',
            ),
            NavigationDestination(
              icon: Icon(Icons.assignment_outlined),
              selectedIcon: Icon(Icons.assignment_rounded),
              label: 'Form DALA',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboardTab(
    String role,
    bool isTier1Manajer,
    bool isTier2High,
  ) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Banner Tier RBAC Aktif
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xFF1E293B),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: const Color(0xFF334155)),
            ),
            child: Row(
              children: [
                const Icon(Icons.security_rounded, color: Color(0xFF38BDF8)),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    isTier1Manajer
                        ? 'RBAC Tier 1 (Full Access - Manajer): Semua menu termasuk Kelola Personil, Pengaturan Sistem & Hapus Laporan aktif.'
                        : isTier2High
                            ? 'RBAC Tier 2 (High Access - $role): Menu Validasi Laporan & Statistik aktif.'
                            : 'RBAC Tier 3 (Standard Access - $role): Menu Operasional Lapangan aktif.',
                    style: const TextStyle(fontSize: 12.5, color: Colors.white),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Kartu Keamanan Perangkat & Sesi (Firestore officer_sessions)
          DeviceSecuritySessionCard(
            loggedInNip: widget.officerNip,
            onForceLogout: _forceLogout,
          ),
          const SizedBox(height: 16),

          // Grafik Monitoring Karhutla
          const KarhutlaChartCard(),
          const SizedBox(height: 16),

          // Komponen Menu Berdasarkan Hak Akses RBAC Multi-Tier
          Card(
            color: const Color(0xFF0F172A),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
              side: const BorderSide(color: Color(0xFF1E293B)),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Menu Akses Jabatan (Multi-Tier RBAC)',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      // Menu Operasional (Tampil untuk Semua Tier 1, 2, 3)
                      _buildActionChip(
                        icon: Icons.map_rounded,
                        label: 'Peta Sebaran',
                        color: const Color(0xFF0284C7),
                      ),
                      // Statistik & Validasi Laporan (Tier 1 & Tier 2)
                      if (UserRole.canViewStatistics(role))
                        _buildActionChip(
                          icon: Icons.bar_chart_rounded,
                          label: 'Statistik Bencana',
                          color: const Color(0xFF7C3AED),
                        ),
                      if (UserRole.canValidateReport(role))
                        _buildActionChip(
                          icon: Icons.fact_check_rounded,
                          label: 'Validasi Laporan',
                          color: const Color(0xFF16A34A),
                        ),
                      // Menu Rahasia Khusus Manajer (Tier 1)
                      if (UserRole.canManagePersonnel(role))
                        _buildActionChip(
                          icon: Icons.groups_rounded,
                          label: 'Kelola Personil',
                          color: const Color(0xFFEA580C),
                        ),
                      if (UserRole.canAccessSystemSettings(role))
                        _buildActionChip(
                          icon: Icons.settings_suggest_rounded,
                          label: 'Pengaturan Sistem',
                          color: const Color(0xFF0D9488),
                        ),
                      if (UserRole.canDeleteReport(role))
                        _buildActionChip(
                          icon: Icons.delete_forever_rounded,
                          label: 'Hapus Laporan',
                          color: const Color(0xFFDC2626),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionChip({
    required IconData icon,
    required String label,
    required Color color,
  }) {
    return ActionChip(
      avatar: Icon(icon, size: 18, color: Colors.white),
      label: Text(
        label,
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.w600,
          fontSize: 12.5,
        ),
      ),
      backgroundColor: color,
      side: BorderSide.none,
      onPressed: () {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Membuka modul: $label')),
        );
      },
    );
  }
}

/// 3. Halaman Form Laporan Harian / Warga dengan SafeArea, SingleChildScrollView,
///    scrollPadding: const EdgeInsets.only(bottom: 100), dan TextInputAction.
class CitizenReportFormTab extends StatefulWidget {
  const CitizenReportFormTab({super.key});

  @override
  State<CitizenReportFormTab> createState() => _CitizenReportFormTabState();
}

class _CitizenReportFormTabState extends State<CitizenReportFormTab> {
  final _reporterController = TextEditingController();
  final _phoneController = TextEditingController();
  final _locationController = TextEditingController();
  final _notesController = TextEditingController();
  String _disasterType = 'Banjir';

  @override
  void dispose() {
    _reporterController.dispose();
    _phoneController.dispose();
    _locationController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewPadding.bottom;

    return SafeArea(
      child: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 80),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Form Laporan Kejadian Bencana',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _disasterType,
                    decoration: const InputDecoration(
                      labelText: 'Jenis Bencana',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(value: 'Banjir', child: Text('Banjir')),
                      DropdownMenuItem(
                        value: 'Tanah Longsor',
                        child: Text('Tanah Longsor'),
                      ),
                      DropdownMenuItem(
                        value: 'Karhutla',
                        child: Text('Karhutla'),
                      ),
                      DropdownMenuItem(
                        value: 'Cuaca Ekstrem',
                        child: Text('Cuaca Ekstrem'),
                      ),
                    ],
                    onChanged: (v) {
                      if (v != null) setState(() => _disasterType = v);
                    },
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _reporterController,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Nama Pelapor',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Nomor Telepon / WhatsApp',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _locationController,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Lokasi / Desa & Kecamatan',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _notesController,
                    maxLines: 3,
                    textInputAction: TextInputAction.done,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Kronologi / Detail Kejadian',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(16, 8, 16, 12 + bottomInset),
            child: SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF15803D),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                onPressed: () {
                  FocusScope.of(context).unfocus();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Laporan kejadian berhasil dikirim!'),
                    ),
                  );
                },
                icon: const Icon(Icons.send_rounded),
                label: const Text(
                  'KIRIM LAPORAN KE PUSDALOPS',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 4. Halaman Form DALA (Damage and Loss Assessment) dengan SafeArea,
///    SingleChildScrollView (padding bottom: 80), scrollPadding: bottom 100,
///    dan tombol bawah yang menghormati MediaQuery.of(context).viewPadding.bottom.
class DalaAssessmentFormTab extends StatefulWidget {
  const DalaAssessmentFormTab({super.key});

  @override
  State<DalaAssessmentFormTab> createState() => _DalaAssessmentFormTabState();
}

class _DalaAssessmentFormTabState extends State<DalaAssessmentFormTab> {
  final _districtController = TextEditingController();
  final _damagedHousesController = TextEditingController();
  final _estimatedLossController = TextEditingController();
  final _evacuationPointController = TextEditingController();

  @override
  void dispose() {
    _districtController.dispose();
    _damagedHousesController.dispose();
    _estimatedLossController.dispose();
    _evacuationPointController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bottomPadding = MediaQuery.of(context).viewPadding.bottom;

    return SafeArea(
      child: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.only(
                left: 16,
                right: 16,
                top: 16,
                bottom: 80,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Formulir Jitupasna / DALA (Kerusakan & Kerugian)',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _districtController,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Kecamatan / Desa Terdampak',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _damagedHousesController,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Jumlah Rumah Rusak (Unit)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _estimatedLossController,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.next,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Estimasi Kerugian (Rp)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _evacuationPointController,
                    textInputAction: TextInputAction.done,
                    scrollPadding: const EdgeInsets.only(bottom: 100),
                    decoration: const InputDecoration(
                      labelText: 'Lokasi Titik Pengungsian Utama',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(16, 8, 16, 12 + bottomPadding),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () {
                      FocusScope.of(context).unfocus();
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Titik pengungsian ditambahkan'),
                        ),
                      );
                    },
                    icon: const Icon(Icons.add_location_alt_outlined),
                    label: const Text('Tambah Titik Pengungsian'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF15803D),
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () {
                      FocusScope.of(context).unfocus();
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Data DALA berhasil disimpan'),
                        ),
                      );
                    },
                    child: const Text('SELANJUTNYA'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
