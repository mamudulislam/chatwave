package com.example.chatwave.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BackendException(val status: Int, val code: String, message: String) : Exception(message)

class BackendApi(
  baseUrl: String,
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
) {
  @Volatile
  var baseUrl: String = baseUrl.trimEnd('/')
    private set

  fun updateBaseUrl(value: String) { baseUrl = value.trimEnd('/') }

  suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
    execute(Request.Builder().url(url(path)).get().build())
  }

  suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
    val requestBody = body.toString().toRequestBody(JSON)
    execute(Request.Builder().url(url(path)).post(requestBody).build())
  }

  private fun url(path: String): String = baseUrl + path

  private fun execute(request: Request): JSONObject {
    client.newCall(request).execute().use { response ->
      val text = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        val code = runCatching { JSONObject(text).optString("code", "request_failed") }
          .getOrDefault("request_failed")
        val message = runCatching { JSONObject(text).optString("message", code) }.getOrDefault(code)
        throw BackendException(response.code, code, message)
      }
      return if (text.isBlank()) JSONObject() else JSONObject(text)
    }
  }

  private companion object {
    val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
