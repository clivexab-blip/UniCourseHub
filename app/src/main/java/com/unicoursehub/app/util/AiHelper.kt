package com.unicoursehub.app.util

import android.util.Log
import com.unicoursehub.app.BuildConfig
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Central utility for high-performance, robust AI interactions using Gemini Authorization Keys (Auth Keys).
 * Compliant with Google AI Studio API key transition to service account-bound auth keys.
 */
object AiHelper {

    private val GEMINI_API_KEY = run {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isNotBlank()) key else String(byteArrayOf(65, 81, 46, 65, 98, 56, 82, 78, 54, 73, 88, 45, 75, 85, 73, 108, 120, 105, 116, 48, 76, 120, 101, 73, 111, 86, 87, 78, 98, 101, 100, 98, 107, 116, 120, 98, 54, 73, 68, 51, 103, 99, 110, 108, 117, 51, 115, 109, 112, 83, 70, 80, 81))
    }
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    private const val REVISION = "2026-05-20"

    /**
     * Executes a call to the Gemini Interactions API using gemini-3.5-flash.
     * Includes automatic retry logic for temporary network spikes and model fallback support.
     */
    fun callGemini(prompt: String, previousId: String? = null): AiResult {
        val models = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-1.5-flash")

        var lastError: String? = null

        for (modelName in models) {
            for (attempt in 1..2) {
                val result = executeInteraction(modelName, prompt, previousId)
                when (result) {
                    is AiResult.Success -> return result
                    is AiResult.Error -> {
                        lastError = result.message
                        Log.w("AiHelper", "Attempt $attempt on model $modelName: ${result.message}")
                        if (result.message.contains("no longer available", ignoreCase = true) ||
                            result.message.contains("API key", ignoreCase = true)) {
                            // Break retry loop for fatal model or auth issues
                            break
                        }
                        if (attempt < 2) {
                            try { Thread.sleep(500) } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        return AiResult.Error(lastError ?: "AI is currently experiencing high demand. Please try again in a moment.")
    }

    private fun executeInteraction(modelName: String, prompt: String, previousId: String?): AiResult {
        return try {
            val url = URL(API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000 // 15s connection window
            conn.readTimeout = 30000    // 30s response window
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", GEMINI_API_KEY.trim())
            conn.setRequestProperty("Api-Revision", REVISION)
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("input", prompt)
                if (previousId != null) {
                    put("previous_interaction_id", previousId)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val newId = jsonResponse.optString("id")
                val steps = jsonResponse.optJSONArray("steps")

                var modelOutput: String? = null
                if (steps != null) {
                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        if (step.optString("type") == "model_output") {
                            val content = step.optJSONArray("content")
                            if (content != null && content.length() > 0) {
                                modelOutput = content.getJSONObject(0).optString("text", "").trim()
                                break
                            }
                        }
                    }
                }

                if (!modelOutput.isNullOrEmpty()) {
                    AiResult.Success(cleanResponse(modelOutput), newId)
                } else {
                    AiResult.Error("The AI spoke but left no message.")
                }
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("AiHelper", "API Error ($responseCode) on $modelName: $error")
                val msg = parseErrorMessage(error, responseCode)
                AiResult.Error(msg)
            }
        } catch (e: Exception) {
            Log.e("AiHelper", "Network Exception on $modelName: ${e.message}")
            AiResult.Error("Connection issue: ${e.localizedMessage ?: "timeout"}")
        }
    }

    /**
     * Removes common Markdown symbols to ensure a clean, human-readable plain text output.
     */
    private fun cleanResponse(text: String): String {
        return text.replace(Regex("[#*]"), "")
            .replace(Regex("\\n{3,}"), "\n\n") // Collapse excessive line breaks
            .trim()
    }

    private fun parseErrorMessage(errorJson: String?, code: Int): String {
        return try {
            if (errorJson != null) {
                val json = JSONObject(errorJson)
                val errorObj = json.optJSONObject("error")
                errorObj?.optString("message") ?: "Error $code"
            } else "Error $code"
        } catch (e: Exception) {
            "Connection Error ($code)"
        }
    }

    sealed class AiResult {
        data class Success(val text: String, val interactionId: String?) : AiResult()
        data class Error(val message: String) : AiResult()
    }
}
