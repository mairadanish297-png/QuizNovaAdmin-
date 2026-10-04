package com.quiznova.admin.ai

import com.google.gson.JsonParser
import com.quiznova.admin.models.QuestionData
import com.quiznova.admin.models.QuestionLang

object ResponseParser {

    fun parseQuestions(response: String): List<QuestionData> {
        var cleaned = response.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        try {
            val jsonArray = JsonParser.parseString(cleaned).asJsonArray
            val questions = mutableListOf<QuestionData>()

            for (element in jsonArray) {
                try {
                    val obj = element.asJsonObject

                    val en = parseLang(obj.getAsJsonObject("en"))
                    val ur = parseLang(obj.getAsJsonObject("ur"))
                    val roman = parseLang(obj.getAsJsonObject("roman"))
                    val correct = obj.get("correct")?.asInt ?: 0
                    val difficulty = obj.get("difficulty")?.asString ?: "easy"
                    val category = obj.get("category")?.asString ?: ""

                    val tags = try {
                        obj.getAsJsonArray("tags").map { it.asString }
                    } catch (_: Exception) {
                        listOf<String>()
                    }

                    questions.add(
                        QuestionData(
                            en = en,
                            ur = ur,
                            roman = roman,
                            correct = correct.coerceIn(0, 3),
                            difficulty = difficulty,
                            category = category,
                            tags = tags,
                            createdBy = "admin_ai"
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed question
                }
            }

            return questions
        } catch (e: Exception) {
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }

    private fun parseLang(obj: com.google.gson.JsonObject?): QuestionLang {
        if (obj == null) return QuestionLang()

        val question = obj.get("question")?.asString ?: ""
        val explanation = obj.get("explanation")?.asString ?: ""

        val options = try {
            obj.getAsJsonArray("options").map { it.asString }
        } catch (_: Exception) {
            listOf("", "", "", "")
        }

        return QuestionLang(
            question = question,
            options = options,
            explanation = explanation
        )
    }
}