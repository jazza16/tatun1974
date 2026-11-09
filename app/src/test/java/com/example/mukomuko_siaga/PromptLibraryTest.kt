package com.example.mukomuko_siaga

import com.example.mukomuko_siaga.data.prompt.PromptRegistry
import com.example.mukomuko_siaga.data.prompt.PromptValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptLibraryTest {

    @Test
    fun `semua kombinasi punya minimal satu template`() {
        PromptRegistry.combinations.forEach { combination ->
            assertTrue(
                "Kombinasi ${combination.disaster} × ${combination.community} kosong",
                combination.templates.isNotEmpty()
            )
        }
    }

    @Test
    fun `semua template lolos validasi editorial`() {
        val failures = PromptRegistry.combinations
            .flatMap { it.templates }
            .mapIndexedNotNull { index, prompt ->
                when (val result = PromptValidator.validate("template#$index", prompt)) {
                    is PromptValidator.Result.Invalid ->
                        "template#$index: ${result.reasons.joinToString("; ")}"
                    PromptValidator.Result.Valid -> null
                }
            }

        assertTrue("Template cacat:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `tidak ada template duplikat`() {
        val templates = PromptRegistry.combinations.flatMap { it.templates }
        assertTrue(
            "Ada template duplikat",
            templates.size == templates.distinct().size
        )
    }
}
