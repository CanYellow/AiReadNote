package com.yueread

import com.yueread.data.AiConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiNetworkManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun sendRequest(config: AiConfig, prompt: String): String {
        return when (config.protocol) {
            "GEMINI" -> requestGemini(config, prompt)
            "OPENAI" -> requestOpenAI(config, prompt)
            else -> throw IllegalArgumentException("不支持的协议类型")
        }
    }

    private fun requestGemini(config: AiConfig, prompt: String): String {
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
        }

        val url = "${config.baseUrl.trimEnd('/')}/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}"
        
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return "请求失败: ${response.code}"
            val responseBody = response.body?.string() ?: return "无返回内容"
            
            val json = JSONObject(responseBody)
            return json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "解析失败: $responseBody"
        }
    }

    private fun requestOpenAI(config: AiConfig, prompt: String): String {
        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val url = "${config.baseUrl.trimEnd('/')}/v1/chat/completions"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return "请求失败: ${response.code}"
            val responseBody = response.body?.string() ?: return "无返回内容"
            
            val json = JSONObject(responseBody)
            return json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content") ?: "解析失败: $responseBody"
        }
    }
}
