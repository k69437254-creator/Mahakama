package com.aram.mahakama.audio

import android.util.Base64
import com.aram.mahakama.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Streams Gemini TTS audio from the Mahakama Cloudflare Worker.
 * Audio callbacks contain raw little-endian PCM16 bytes at 24 kHz, mono.
 */
internal class GeminiTtsClient(
    private val appToken: String = BuildConfig.MAHAKAMA_TTS_TOKEN
) {
    companion object {
        private const val ENDPOINT =
            "https://mahakama-gemini-tts.k69437254.workers.dev/v1/tts"
        private const val MODEL = "gemini-3.8-flash-lite-tts"
        private const val DEFAULT_VOICE = "Kore"
        private const val DEFAULT_STYLE =
            "Warm, calm, deep and expressive Arabic narration with clear pronunciation."
        private const val USER_AGENT = "Mahakama-TTS-Test/1.0"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun checkHealth(onReady: () -> Unit, onError: (Throwable) -> Unit): Call {
        val request = Request.Builder()
            .url("https://mahakama-gemini-tts.k69437254.workers.dev/health")
            .header("User-Agent", USER_AGENT).get().build()
        val call = http.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (!call.isCanceled()) onError(error)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!call.isCanceled()) {
                        if (it.isSuccessful) onReady()
                        else onError(IOException("فشل فحص خدمة الصوت: HTTP ${it.code}"))
                    }
                }
            }
        })
        return call
    }

    fun stream(
        text: String,
        voice: String = DEFAULT_VOICE,
        style: String = DEFAULT_STYLE,
        onAudioChunk: (ByteArray) -> Boolean,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): Call {
        require(text.isNotBlank()) { "النص الصوتي فارغ" }
        require(appToken.isNotBlank()) {
            "رمز مصادقة خدمة الصوت غير مضبوط في بيئة البناء"
        }

        val payload = JSONObject()
            .put("text", text)
            .put("model", MODEL)
            .put("voice", voice)
            .put("style", style)

        val body = payload.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer $appToken")
            .header("Accept", "text/event-stream")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()

        val call = http.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (!call.isCanceled()) onError(error)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { res ->
                        if (!res.isSuccessful) {
                            val detail = res.body?.string().orEmpty().take(500)
                            throw IOException(
                                "خدمة Gemini أعادت HTTP ${res.code}: $detail"
                            )
                        }

                        val source = res.body?.source()
                            ?: throw IOException("استجابة الصوت فارغة")

                        var receivedAudio = false
                            while (!call.isCanceled() && !source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue

                            val data = line.substring(5).trim()
                            if (data.isEmpty() || data == "[DONE]") continue

                            val event = try {
                                JSONObject(data)
                            } catch (_: Exception) {
                                continue
                            }

                            val serverError = event.optJSONObject("error")
                            if (serverError != null) {
                                throw IOException(
                                    "خطأ من خدمة Gemini: ${serverError}"
                                )
                            }

                            val delta = event.optJSONObject("delta")
                                ?: continue

                            if (delta.optString("type") != "audio") continue

                            val encoded = delta.optString("data")
                            if (encoded.isBlank()) continue

                            val audioBytes = try {
                                Base64.decode(encoded, Base64.DEFAULT)
                            } catch (error: IllegalArgumentException) {
                                throw IOException(
                                    "تعذر فك ترميز مقطع الصوت Base64",
                                    error
                                )
                            }

                            if (audioBytes.isNotEmpty() && !call.isCanceled()) {
                            receivedAudio = true
                            if (!onAudioChunk(audioBytes)) {
                                call.cancel()
                                return
                            }
                        }
                        }

                        if (!call.isCanceled()) {
                        if (!receivedAudio) throw IOException("لم تصل بيانات صوتية من Gemini")
                        onComplete()
                    }
                    }
                } catch (error: Throwable) {
                    if (!call.isCanceled()) onError(error)
                }
            }
        })

        return call
    }
}
