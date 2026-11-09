package id.go.mukomuko.sipagarkita.feature.pesanvisual.data

enum class DisasterType(val label: String) {
    GEMPA("Gempa Bumi & Tsunami"),
    BANJIR("Banjir & Abrasi"),
    KARHUTLA("Kebakaran Hutan & Lahan"),
    LONGSOR("Tanah Longsor"),
    KEKERINGAN("Kekeringan"),
    CUACA_EKSTREM_LAUT("Cuaca Ekstrem di Laut")
}

object PromptLibrary {
    data class PromptEntry(val disaster: DisasterType, val prompt: String)

    val entries: List<PromptEntry> = listOf(
        PromptEntry(
            disaster = DisasterType.GEMPA,
            prompt = "A high-quality educational disaster preparedness poster illustration set in Mukomuko, Bengkulu. In the lower two-thirds of the frame, disciplined coastal villagers and youth incorporate the swift, synchronized steps inspired by the traditional Tari Gandai into an orderly earthquake evacuation drill, walking purposefully towards high ground and tsunami evacuation shelters away from the Indian Ocean coastline. Dramatic early morning coastal sky above. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        ),
        PromptEntry(
            disaster = DisasterType.BANJIR,
            prompt = "A cinematic public awareness poster illustration set in coastal Mukomuko. In the lower two-thirds of the frame, local residents and BPBD responders unite in the spirit of gotong royong along the Mukomuko riverbanks and shoreline, stacking sandbags and planting coastal vegetation to protect coastal stilt houses from seasonal river flooding and tidal sea abrasion. Warm dramatic lighting with reflections on damp ground. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        ),
        PromptEntry(
            disaster = DisasterType.KARHUTLA,
            prompt = "A detailed documentary-style preparedness poster illustration set in the peatland areas of Mukomuko. In the lower two-thirds of the frame, forest patrol volunteers and villagers demonstrate community fire mitigation, utilizing traditional woven niqhung bamboo tools alongside modern water knapsacks to damp dry brush and create firebreaks before dry season. Clear smoky horizon with realistic atmospheric lighting. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        ),
        PromptEntry(
            disaster = DisasterType.LONGSOR,
            prompt = "An inspiring environmental disaster mitigation poster illustration set in the hilly terrain of Mukomuko. In the lower two-thirds of the frame, villagers reinforce a hillside terrace using deep-rooted vetiver grass and stone retaining walls to protect a distinctive wooden Mukomuko traditional stilt house (rumah adat) from dangerous soil landslides during torrential tropical rains. Lush green environment with cloudy sky. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        ),
        PromptEntry(
            disaster = DisasterType.KEKERINGAN,
            prompt = "A vibrant humanitarian resilience poster illustration set in rural Mukomuko. In the lower two-thirds of the frame, local agricultural community members and elders gather near traditional water reservoirs, practicing communal water conservation, rain harvesting, and cooperative distribution of clean water reserves during extended drought. Warm golden sunset glow over the rural landscape. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        ),
        PromptEntry(
            disaster = DisasterType.CUACA_EKSTREM_LAUT,
            prompt = "A compelling maritime safety poster illustration on the coast of Mukomuko. In the lower two-thirds of the frame, traditional coastal fishermen known locally as Beno work together to haul their colorful wooden outrigger boats safely up the beach, securing life vests and checking coastal radio alerts as dark storm clouds and high ocean swells approach. Moody ocean spray and dynamic waves. Leave the top-right corner empty for institutional logos. Do not include any text, logos, or watermarks."
        )
    )
}
