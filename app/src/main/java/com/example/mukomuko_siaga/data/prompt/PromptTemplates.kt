package com.example.mukomuko_siaga.data.prompt

/**
 * Kumpulan template prompt final.
 *
 * Aturan editorial:
 * - Setiap template WAJIB memuat [PromptMarkers.SAFE_AREA] dan [PromptMarkers.NO_TEXT].
 * - Setiap template WAJIB menyebut "Mukomuko".
 * - Jangan menyebut BPBD / Lambang / logo di dalam prompt (logo di-overlay di Compose).
 * - Validasi otomatis ada di [PromptValidator].
 */
object PromptTemplates {

    // ─────────────────────────────────────────────────────────────
    // GEMPA BUMI & TSUNAMI
    // ─────────────────────────────────────────────────────────────

    val GEMPA_UMUM_A = """
        A warm educational illustration of a coastal village in Mukomuko, West Sumatra.
        In the lower two-thirds of the frame, villagers calmly practice an evacuation
        toward higher ground, guided by local elders; traditional stilt houses and a
        wooden fishing boat are visible. The Indian Ocean is calm in the background
        with a subtle signal buoy.
        ${PromptMarkers.SAFE_AREA}
        Warm golden hour lighting, tropical colors, community resilience mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val GEMPA_UMUM_B = """
        A symbolic illustration of the Mukomuko coastal community performing a
        traditional 'tolak bala' thanksgiving prayer on the beach, positioned in the
        lower two-thirds of the frame. Elders and families stand together facing the
        sea with calm awareness. A small warning siren pole is subtly visible.
        ${PromptMarkers.SAFE_AREA}
        Soft dawn lighting, gentle waves, respectful atmosphere.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val GEMPA_NELAYAN_A = """
        A dramatic yet educational illustration of Mukomuko fishermen on the beach at
        dawn, positioned in the lower two-thirds of the frame. An elder fisherman
        points toward the receding sea and warns younger fishermen about 'Beno', the
        local term for abnormal ocean conditions. Their wooden boats are pulled onto
        higher sand. Traditional stilt houses are visible nearby.
        ${PromptMarkers.SAFE_AREA}
        Golden dawn light, tense but alert mood, tropical coastal palette.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────
    // BANJIR & ABRASI
    // ─────────────────────────────────────────────────────────────

    val BANJIR_UMUM_A = """
        Photorealistic scene of a Mukomuko community practicing flood preparedness.
        In the lower two-thirds of the frame, residents work together clearing a river
        channel using traditional bamboo winnowing trays ('niqhung'), while women
        prepare simple meals nearby. Lush tropical vegetation and stilt houses
        surround the scene.
        ${PromptMarkers.SAFE_AREA}
        Gotong royong atmosphere, natural daylight, earthy green and brown tones.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val BANJIR_UMUM_B = """
        An educational illustration of Mukomuko coastal villagers planting mangrove
        seedlings along the shoreline, occupying the lower two-thirds of the frame.
        Adults and children work together; traditional boat and coastal houses in
        soft background. The sea is calm and blue-green.
        ${PromptMarkers.SAFE_AREA}
        Warm afternoon light, hopeful mood, vibrant tropical palette.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val BANJIR_NELAYAN_A = """
        Photorealistic scene of Mukomuko fishermen and coastal families reinforcing a
        sandbag barrier along the shoreline, in the lower two-thirds of the frame.
        Women and children carry woven baskets of sand; traditional boats and wooden
        houses are visible behind. The sea is choppy with visible waves.
        ${PromptMarkers.SAFE_AREA}
        Overcast natural lighting, muted coastal greens and blues, community
        resilience mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val BANJIR_PETANI_A = """
        An illustration of Mukomuko farmers in a flooded rice field, working together
        to repair an embankment using traditional woven bamboo baskets, in the lower
        two-thirds of the frame. A village leader directs the work; palm trees and a
        small wooden bridge are visible in the background.
        ${PromptMarkers.SAFE_AREA}
        Overcast tropical light, earthy green and brown tones, gotong royong
        atmosphere.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────
    // KEBAKARAN HUTAN & LAHAN
    // ─────────────────────────────────────────────────────────────

    val KARHUTLA_UMUM_A = """
        Symbolic illustration of a wise village elder in Mukomuko explaining fire
        safety to children, positioned in the lower two-thirds of the frame. In the
        background, the traditional 'Gembieng-Qhimbo' ritual of burning coconut
        shells in a controlled manner, alongside a cleared firebreak and green forest
        edge.
        ${PromptMarkers.SAFE_AREA}
        Blend of local folk art and modern educational poster style, warm earthy tones
        of brown, green, and orange.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val KARHUTLA_UMUM_B = """
        Documentary-style illustration of a Mukomuko village fire patrol team walking
        along a forest edge, positioned in the lower two-thirds of the frame. They
        carry simple traditional tools and water containers; smoke haze is subtly
        visible in the far background.
        ${PromptMarkers.SAFE_AREA}
        Dry season afternoon light, dusty warm tones, alert but calm mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val KARHUTLA_PETANI_A = """
        An educational illustration of Mukomuko farmers practicing safe land clearing
        without burning, in the lower two-thirds of the frame. A group of farmers
        compost dried plant waste in a neat pile while a village leader explains the
        method; a green forest edge with a cleared firebreak is visible in the
        background.
        ${PromptMarkers.SAFE_AREA}
        Late afternoon warm light, earthy tones of brown, green, and orange,
        cooperative mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────
    // TANAH LONGSOR
    // ─────────────────────────────────────────────────────────────

    val LONGSOR_UMUM_A = """
        Creative public service illustration for Mukomuko, featuring a dancer in
        traditional 'Tari Gandai' costume, gracefully posed in the lower two-thirds
        of the frame. The dancer's movement is juxtaposed with a steep hillside in
        the background showing early landslide signs (cracks in the soil, tilted
        trees).
        ${PromptMarkers.SAFE_AREA}
        Modern vector illustration style with a color palette inspired by traditional
        Mukomuko woven fabrics (deep red, gold, cream).
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val LONGSOR_UMUM_B = """
        Photorealistic scene of Mukomuko villagers inspecting and marking soil cracks
        on a hillside slope, in the lower two-thirds of the frame. A community leader
        points at the crack while others take notes; traditional houses visible far
        below.
        ${PromptMarkers.SAFE_AREA}
        Overcast natural lighting, muted tropical greens, serious but cooperative
        mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val LONGSOR_PETANI_A = """
        Photorealistic scene of Mukomuko farmers inspecting a hillside farmland with
        early landslide signs (soil cracks, tilted trees), in the lower two-thirds of
        the frame. Two farmers discuss the findings while one takes notes; a small
        farm hut and terraced fields are visible below.
        ${PromptMarkers.SAFE_AREA}
        Overcast natural lighting, muted tropical greens, serious and cooperative
        mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────
    // KEKERINGAN
    // ─────────────────────────────────────────────────────────────

    val KEKERINGAN_UMUM_A = """
        An educational illustration of a Mukomuko village during dry season, in the
        lower two-thirds of the frame. Villagers gather around a shallow well with
        low water, sharing water containers and discussing conservation. Traditional
        wooden houses and dry palm trees are visible behind.
        ${PromptMarkers.SAFE_AREA}
        Harsh dry-season sunlight, warm ochre and dusty green tones, cooperative and
        resilient mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    val KEKERINGAN_PETANI_A = """
        Photorealistic scene of Mukomuko farmers inspecting a dry rice field with
        visible soil cracks, positioned in the lower two-thirds of the frame. They
        carry traditional farming tools and a woven basket; a small water reservoir
        and palm trees are visible in the background.
        ${PromptMarkers.SAFE_AREA}
        Harsh dry season lighting, warm ochre and dusty green tones, cautious and
        resilient mood.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────
    // CUACA EKSTREM DI LAUT
    // ─────────────────────────────────────────────────────────────

    val CUACA_LAUT_NELAYAN_A = """
        An educational illustration of Mukomuko fishermen checking weather signs and
        traditional cloud patterns before setting sail, in the lower two-thirds of
        the frame. An elder fisherman gestures toward the horizon while a younger one
        observes; a wooden boat rests on the beach. Distant dark clouds gather over
        the sea.
        ${PromptMarkers.SAFE_AREA}
        Early morning tropical light, attentive and cautious atmosphere, warm seaside
        palette.
        ${PromptMarkers.NO_TEXT}
    """.trimIndent()
}
