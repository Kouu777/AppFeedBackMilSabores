package com.example.proyectomilsabores.language

class SentimentAnalyzer {


    suspend fun analyzeSentiment(text: String): SentimentResult {
        return try {
            // Simulación de análisis
            Thread.sleep(500)


            val positiveWords = listOf("excelente", "bueno", "genial", "increíble", "perfecto")
            val negativeWords = listOf("malo", "horrible", "terrible", "pésimo", "decepcionante")

            val lowerText = text.lowercase()
            val positiveCount = positiveWords.count { lowerText.contains(it) }
            val negativeCount = negativeWords.count { lowerText.contains(it) }

            val score = when {
                positiveCount > negativeCount -> 0.8f
                negativeCount > positiveCount -> 0.2f
                else -> 0.5f
            }

            SentimentResult(
                score = score,
                magnitude = text.length * 0.01f,
                isPositive = score > 0.6f
            )
        } catch (e: Exception) {
            SentimentResult(error = e.message)
        }
    }
}

data class SentimentResult(
    val score: Float = 0.5f,
    val magnitude: Float = 0f,
    val isPositive: Boolean = false,
    val error: String? = null
)