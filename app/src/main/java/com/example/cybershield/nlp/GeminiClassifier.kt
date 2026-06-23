package com.example.cybershield.nlp

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString

object GeminiClassifier {
    const val API_KEY = "YOUR_API_KEY_HERE"
    private val client = HttpClient()

    suspend fun classify(text: String, apiKey: String = API_KEY): ClassificationResult {
        val url = "https://api.openai.com/v1/chat/completions"
        
        val prompt = """
            Analyze this message for cyberbullying, harassment, hate speech, or physical threats.
            Message: "$text"
            
            Respond strictly in valid JSON format with the following schema. Do not include markdown block markers (such as ```json) or extra text:
            {
               "severity": "NONE" or "LOW" or "MEDIUM" or "HIGH" or "CRITICAL",
               "reason": "a brief 3-5 word description of the category detected"
            }
        """.trimIndent()

        val requestBody = """
            {
              "model": "gpt-4o-mini",
              "messages": [
                {
                  "role": "user",
                  "content": ${Json.encodeToString(prompt)}
                }
              ],
              "temperature": 0.0
            }
        """.trimIndent()

        return try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(requestBody)
            }
            val responseText = response.bodyAsText()
            if (response.status.value != 200) {
                throw Exception("HTTP status ${response.status.value}")
            }
            
            // Extract text response from OpenAI chat completions choices
            val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
            val choices = jsonResponse["choices"]?.jsonArray
            val firstChoice = choices?.firstOrNull()?.jsonObject
            val messageObj = firstChoice?.get("message")?.jsonObject
            val textResultRaw = messageObj?.get("content")?.jsonPrimitive?.content ?: ""
            
            // Clean JSON out of Markdown codeblocks if any
            val cleanedJson = textResultRaw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val resultObj = Json.parseToJsonElement(cleanedJson).jsonObject
            val severityStr = resultObj["severity"]?.jsonPrimitive?.content ?: "NONE"
            val reason = resultObj["reason"]?.jsonPrimitive?.content ?: "None"

            val severity = try {
                Severity.valueOf(severityStr.uppercase())
            } catch (e: Exception) {
                Severity.NONE
            }

            ClassificationResult(
                severity = severity,
                isFlagged = severity.ordinal >= Severity.MEDIUM.ordinal,
                reason = "ChatGPT: $reason"
            )
        } catch (e: Exception) {
            // Fallback to local classifier on error
            BullyingClassifier.classify(text, "Medium")
        }
    }


}
