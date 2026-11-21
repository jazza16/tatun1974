import 'package:flutter/material.dart';

/// Kartu Grafik Monitoring Karhutla dengan tata letak sumbu Y dan badge kanan
/// yang sejajar, memiliki padding memadai, dan bebas dari teks terpotong ("Ki"/"ni").
class KarhutlaChartCard extends StatelessWidget {
  const KarhutlaChartCard({super.key});

  @override
  Widget build(BuildContext context) {
    final List<Map<String, dynamic>> districtHotspots = [
      {'kecamatan': 'Kota Mukomuko', 'count': 1, 'status': 'Aman'},
      {'kecamatan': 'Lubuk Pinang', 'count': 2, 'status': 'Waspada'},
      {'kecamatan': 'Ipuh', 'count': 4, 'status': 'Siaga'},
      {'kecamatan': 'Penarik', 'count': 3, 'status': 'Waspada'},
      {'kecamatan': 'Teramang Jaya', 'count': 1, 'status': 'Aman'},
    ];
    const int maxScale = 5;

    return Card(
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
            Row(
              children: [
                const Icon(
                  Icons.local_fire_department_rounded,
                  color: Color(0xFFF97316),
                  size: 22,
                ),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'Monitoring Karhutla & Titik Panas (Hotspot)',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFF1E293B),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Text(
                    'Kondisi Terkini',
                    maxLines: 1,
                    softWrap: false,
                    overflow: TextOverflow.visible,
                    style: TextStyle(
                      color: Color(0xFF38BDF8),
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ...districtHotspots.map((item) {
              final int count = item['count'] as int;
              final double ratio = (count / maxScale).clamp(0.0, 1.0);
              final Color barColor = count >= 4
                  ? const Color(0xFFEF4444)
                  : count >= 2
                      ? const Color(0xFFF59E0B)
                      : const Color(0xFF22C55E);

              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    SizedBox(
                      width: 105,
                      child: Text(
                        item['kecamatan'] as String,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: Color(0xFFCBD5E1),
                          fontSize: 12,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: LinearProgressIndicator(
                          value: ratio,
                          minHeight: 12,
                          backgroundColor: const Color(0xFF1E293B),
                          valueColor: AlwaysStoppedAnimation<Color>(barColor),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Container(
                      constraints: const BoxConstraints(minWidth: 74),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: barColor.withValues(alpha: 0.18),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: barColor.withValues(alpha: 0.5),
                        ),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        '$count Titik',
                        maxLines: 1,
                        softWrap: false,
                        style: TextStyle(
                          color: barColor,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}
