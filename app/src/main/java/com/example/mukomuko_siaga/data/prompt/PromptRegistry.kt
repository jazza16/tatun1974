package com.example.mukomuko_siaga.data.prompt

import com.example.mukomuko_siaga.data.model.CommunityGroup
import com.example.mukomuko_siaga.data.model.DisasterType

data class PromptCombination(
    val disaster: DisasterType,
    val community: CommunityGroup,
    val templates: List<String>
)

object PromptRegistry {

    /** Semua template dalam satu map, khusus untuk validasi. */
    private val allTemplates: Map<String, String> = mapOf(
        "GEMPA_UMUM_A" to PromptTemplates.GEMPA_UMUM_A,
        "GEMPA_UMUM_B" to PromptTemplates.GEMPA_UMUM_B,
        "GEMPA_NELAYAN_A" to PromptTemplates.GEMPA_NELAYAN_A,

        "BANJIR_UMUM_A" to PromptTemplates.BANJIR_UMUM_A,
        "BANJIR_UMUM_B" to PromptTemplates.BANJIR_UMUM_B,
        "BANJIR_NELAYAN_A" to PromptTemplates.BANJIR_NELAYAN_A,
        "BANJIR_PETANI_A" to PromptTemplates.BANJIR_PETANI_A,

        "KARHUTLA_UMUM_A" to PromptTemplates.KARHUTLA_UMUM_A,
        "KARHUTLA_UMUM_B" to PromptTemplates.KARHUTLA_UMUM_B,
        "KARHUTLA_PETANI_A" to PromptTemplates.KARHUTLA_PETANI_A,

        "LONGSOR_UMUM_A" to PromptTemplates.LONGSOR_UMUM_A,
        "LONGSOR_UMUM_B" to PromptTemplates.LONGSOR_UMUM_B,
        "LONGSOR_PETANI_A" to PromptTemplates.LONGSOR_PETANI_A,

        "KEKERINGAN_UMUM_A" to PromptTemplates.KEKERINGAN_UMUM_A,
        "KEKERINGAN_PETANI_A" to PromptTemplates.KEKERINGAN_PETANI_A,

        "CUACA_LAUT_NELAYAN_A" to PromptTemplates.CUACA_LAUT_NELAYAN_A
    )

    val combinations: List<PromptCombination> = buildList {
        // ── GEMPA ──────────────────────────────────────────────
        add(
            PromptCombination(
                DisasterType.GEMPA, CommunityGroup.UMUM,
                listOf(PromptTemplates.GEMPA_UMUM_A, PromptTemplates.GEMPA_UMUM_B)
            )
        )
        add(
            PromptCombination(
                DisasterType.GEMPA, CommunityGroup.NELAYAN,
                listOf(PromptTemplates.GEMPA_NELAYAN_A)
            )
        )

        // ── BANJIR ─────────────────────────────────────────────
        add(
            PromptCombination(
                DisasterType.BANJIR, CommunityGroup.UMUM,
                listOf(PromptTemplates.BANJIR_UMUM_A, PromptTemplates.BANJIR_UMUM_B)
            )
        )
        add(
            PromptCombination(
                DisasterType.BANJIR, CommunityGroup.NELAYAN,
                listOf(PromptTemplates.BANJIR_NELAYAN_A)
            )
        )
        add(
            PromptCombination(
                DisasterType.BANJIR, CommunityGroup.PETANI,
                listOf(PromptTemplates.BANJIR_PETANI_A)
            )
        )

        // ── KARHUTLA ───────────────────────────────────────────
        add(
            PromptCombination(
                DisasterType.KARHUTLA, CommunityGroup.UMUM,
                listOf(PromptTemplates.KARHUTLA_UMUM_A, PromptTemplates.KARHUTLA_UMUM_B)
            )
        )
        add(
            PromptCombination(
                DisasterType.KARHUTLA, CommunityGroup.PETANI,
                listOf(PromptTemplates.KARHUTLA_PETANI_A)
            )
        )

        // ── LONGSOR ────────────────────────────────────────────
        add(
            PromptCombination(
                DisasterType.LONGSOR, CommunityGroup.UMUM,
                listOf(PromptTemplates.LONGSOR_UMUM_A, PromptTemplates.LONGSOR_UMUM_B)
            )
        )
        add(
            PromptCombination(
                DisasterType.LONGSOR, CommunityGroup.PETANI,
                listOf(PromptTemplates.LONGSOR_PETANI_A)
            )
        )

        // ── KEKERINGAN ─────────────────────────────────────────
        add(
            PromptCombination(
                DisasterType.KEKERINGAN, CommunityGroup.UMUM,
                listOf(PromptTemplates.KEKERINGAN_UMUM_A)
            )
        )
        add(
            PromptCombination(
                DisasterType.KEKERINGAN, CommunityGroup.PETANI,
                listOf(PromptTemplates.KEKERINGAN_PETANI_A)
            )
        )

        // ── CUACA EKSTREM LAUT ─────────────────────────────────
        add(
            PromptCombination(
                DisasterType.CUACA_EKSTREM_LAUT, CommunityGroup.NELAYAN,
                listOf(PromptTemplates.CUACA_LAUT_NELAYAN_A)
            )
        )
    }

    init {
        // Fail fast: aplikasi tidak akan jalan jika ada template cacat.
        PromptValidator.validateAll(allTemplates)
    }
}
