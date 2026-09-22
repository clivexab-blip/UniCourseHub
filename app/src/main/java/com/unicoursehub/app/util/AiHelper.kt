package com.unicoursehub.app.util

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

import com.unicoursehub.app.BuildConfig

/**
 * Central utility for high-performance, robust AI interactions.
 */
object AiHelper {
    
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
    private const val REVISION = "2026-05-20"

    /**
     * Executes a high-speed call to the Gemini Interactions API.
     * Includes optimized prompt handling and granular error reporting.
     */
    fun callGemini(prompt: String, previousId: String? = null): AiResult {
        return try {
            val url = URL(API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            // INCREASED TIMEOUTS: Don't cut off the AI early
            conn.connectTimeout = 30000 // 30s connection window
            conn.readTimeout = 90000    // 90s response window (for complex AI thoughts)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", GEMINI_API_KEY.trim())
            conn.setRequestProperty("Api-Revision", REVISION)
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("model", "gemini-3.8-flash") // Fastest model
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
                val steps = jsonResponse.getJSONArray("steps")
                
                var modelOutput: String? = null
                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    if (step.getString("type") == "model_output") {
                        modelOutput = step.getJSONArray("content").getJSONObject(0).getString("text").trim()
                        break
                    }
                }
                
                if (modelOutput != null) {
                    AiResult.Success(cleanResponse(modelOutput), newId)
                } else {
                    AiResult.Error("The AI spoke but left no message. Please try again.")
                }
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("AiHelper", "API Error ($responseCode): $error")
                val msg = parseErrorMessage(error, responseCode)
                AiResult.Error(msg)
            }
        } catch (e: Exception) {
            Log.e("AiHelper", "Network Error: ${e.message}")
            AiResult.Error("Thinking deeply... The connection is slow, please wait a moment and try again.")
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
