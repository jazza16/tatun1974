package com.example.mukomuko_siaga.data.model

enum class DisasterType(val label: String, val shortText: String) {
    GEMPA("Gempa Bumi & Tsunami", "SIAGA GEMPA"),
    BANJIR("Banjir & Abrasi", "WASPADA BANJIR"),
    KARHUTLA("Kebakaran Hutan & Lahan", "CEGAH KARHUTLA"),
    LONGSOR("Tanah Longsor", "WASPADA LONGSOR"),
    KEKERINGAN("Kekeringan", "HEMAT AIR"),
    CUACA_EKSTREM_LAUT("Cuaca Ekstrem di Laut", "WASPADA MELAUT")
}
