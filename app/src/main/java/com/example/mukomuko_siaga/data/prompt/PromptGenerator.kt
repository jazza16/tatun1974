package com.example.mukomuko_siaga.data.prompt

import com.example.mukomuko_siaga.data.model.CommunityGroup
import com.example.mukomuko_siaga.data.model.DisasterType

data class GeneratedPrompt(
    val prompt: String,
    val disaster: DisasterType,
    val community: CommunityGroup,
    val shortText: String
)

class PromptGenerator(
    private val combinationBag: ShuffleBag<PromptCombination> =
        ShuffleBag(PromptRegistry.combinations)
) {
    fun generate(): GeneratedPrompt {
        val combination = combinationBag.next()
        val template = combination.templates.random()

        return GeneratedPrompt(
            prompt = template,
            disaster = combination.disaster,
            community = combination.community,
            shortText = combination.disaster.shortText
        )
    }
}
