package com.quiznova.admin.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiClient(private val apiKey: String, private val provider: String = "groq") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun generateQuestions(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                when (provider) {
                    "groq" -> generateWithGroq(prompt)
                    "gemini" -> generateWithGemini(prompt)
                    else -> Result.failure(Exception("Unknown provider: $provider"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun generateWithGroq(prompt: String): Result<String> {
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val body = """
        {
            "model": "llama-3.3-70b-versatile",
            "messages": [
                {
                    "role": "system",
                    "content": "You are a quiz question generator. Always respond with valid JSON only. No markdown, no code blocks, no extra text."
                },
                {
                    "role": "user",
                    "content": ${gson.toJson(prompt)}
                }
            ],
            "temperature": 0.7,
            "max_tokens": 8000
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return Result.failure(Exception("Groq Error ${response.code}: $responseBody"))
        }

        val json = JsonParser.parseString(responseBody).asJsonObject
        val content = json
            .getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString

        return Result.success(content)
    }

    private fun generateWithGemini(prompt: String): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val body = """
        {
            "contents": [{
                "parts": [{
                    "text": ${gson.toJson(prompt)}
                }]
            }],
            "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 8192
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return Result.failure(Exception("Gemini Error ${response.code}: $responseBody"))
        }

        val json = JsonParser.parseString(responseBody).asJsonObject
        val content = json
            .getAsJsonArray("candidates")
            .get(0).asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).asJsonObject
            .get("text").asString

        return Result.success(content)
    }
}