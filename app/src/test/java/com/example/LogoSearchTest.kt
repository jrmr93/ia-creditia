package com.example

import com.example.util.GeminiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoSearchTest {

    @Test
    fun testGeminiServiceSearchLogoCandidates() = runBlocking {
        val testNames = listOf(
            "ANT",
            "TIENDAS INDUSTRIALES ASOCIADAS TIA S.A.",
            "BANCO PICHINCHA C.A.",
            "Supermaxi",
            "Sana Sana"
        )

        for (name in testNames) {
            val results = GeminiService.searchLogoCandidates(
                apiKey = "",
                modelName = "",
                query = name
            )

            println("====== GeminiService.searchLogoCandidates('$name') ======")
            println("Total candidates returned: ${results.size}")
            val sources = results.groupBy { it.source }.mapValues { it.value.size }
            println("Sources: $sources")
            results.take(3).forEach {
                println("  [${it.source}] [${it.domain}] ${it.title} -> ${it.url.take(80)}")
            }

            assertTrue("Debe encontrar más de 5 candidatos para $name", results.size >= 5)
            assertTrue("Debe haber fuentes diversas para $name", sources.keys.size >= 2)
        }
    }
}
