package com.quiznova.admin.ai

object PromptBuilder {

    fun buildGeneratePrompt(
        category: String,
        difficulty: String,
        count: Int,
        languages: String,
        customTopic: String = "",
        type: String = "text"
    ): String {
        val topicLine = if (customTopic.isNotEmpty()) "\nFocus specifically on: $customTopic" else ""
        val typeLine = if (type != "text") "\nThis is a $type quiz. Ensure questions are suitable for $type format." else ""

        val langInstructions = when (languages) {
            "en" -> "Provide ONLY English (en)."
            "en_ur" -> """
                Provide English (en) AND Urdu (ur).
                URDU RULES:
                - Use simple Urdu that students can understand
                - Mix common English words where natural
                - Use proper Urdu script
            """.trimIndent()
            else -> """
                Provide all 3 languages: English (en), Urdu (ur), and Roman Urdu (roman).
                ENGLISH: Clear, simple English.
                URDU: Simple Urdu with common English words mixed in. Use proper Urdu script.
                ROMAN URDU: Same content as Urdu but written in English letters. Use casual Pakistani style.
            """.trimIndent()
        }

        return """
You are a quiz question generator. Generate exactly $count $difficulty level multiple choice questions about $category.$topicLine$typeLine

$langInstructions

RULES:
- Each question must have exactly 4 options
- Only 1 correct answer
- Questions must be factually accurate
- Explanations should be brief and clear
- Options should be plausible

Return ONLY a valid JSON array. No markdown, no code blocks, no extra text.

Format:
[
  {
    "en": {
      "question": "Question text here?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "explanation": "Brief explanation"
    },
    "ur": {
      "question": "سوال یہاں لکھیں؟",
      "options": ["آپشن اے", "آپشن بی", "آپشن سی", "آپشن ڈی"],
      "explanation": "مختصر وضاحت"
    },
    "roman": {
      "question": "Sawaal yahan likhein?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "explanation": "Mukhtasar wazahat"
    },
    "correct": 0,
    "difficulty": "$difficulty",
    "category": "$category",
    "type": "$type",
    "tags": ["tag1", "tag2"]
  }
]

Generate exactly $count questions now.
        """.trimIndent()
    }
}