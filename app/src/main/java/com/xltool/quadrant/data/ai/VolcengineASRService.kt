package com.xltool.quadrant.data.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * 火山引擎(Volcengine)语音识别服务
 * 大模型录音文件极速版 API
 */
class VolcengineASRService {

    companion object {
        // 火山引擎语音识别API地址
        private const val API_URL = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/submit"
        
        // 用户凭证
        private const val APP_ID = "2026452272"
        private const val ACCESS_TOKEN = "q4qTTftP35LFOwgtiyvDD4NAWXicLZmB"
        private const val SECRET_KEY = "Fi9aHPr-hg3gf-HgzmlJAYwZeun1uNiW"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 将音频文件转录为文字
     * @param audioFile 音频文件(支持 m4a, wav, mp3, flac 等格式)
     * @return 转录后的文字内容
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!audioFile.exists()) {
                    return@withContext Result.failure(Exception("音频文件不存在"))
                }

                if (audioFile.length() == 0L) {
                    return@withContext Result.failure(Exception("音频文件为空"))
                }

                // 读取音频文件并转为Base64
                val audioBytes = audioFile.readBytes()
                val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

                // 确定音频格式
                val audioFormat = getAudioFormat(audioFile.name)

                // 构建请求体
                val requestBody = buildRequestBody(audioBase64, audioFormat)

                // 构建请求
                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Api-App-Key", APP_ID)
                    .addHeader("X-Api-Access-Key", ACCESS_TOKEN)
                    .addHeader("X-Api-Resource-Id", "volc.bigasr.auc")
                    .addHeader("X-Api-Request-Id", UUID.randomUUID().toString())
                    .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                // 发送请求
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    parseResponse(responseBody)
                } else {
                    Result.failure(Exception("语音识别失败: ${response.code} - $responseBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("语音识别出错: ${e.message}"))
            }
        }
    }

    /**
     * 构建请求体
     */
    private fun buildRequestBody(audioBase64: String, audioFormat: String): JSONObject {
        return JSONObject().apply {
            put("app", JSONObject().apply {
                put("appid", APP_ID)
                put("cluster", "volc_auc_common")
            })
            put("user", JSONObject().apply {
                put("uid", "xltool_user")
            })
            put("request", JSONObject().apply {
                put("model_name", "bigmodel")
                put("enable_itn", true)  // 启用逆文本正则化（数字转中文等）
                put("enable_punc", true) // 启用标点符号
                put("result_type", "single") // 单次返回完整结果
            })
            put("audio", JSONObject().apply {
                put("format", audioFormat)
                put("data", audioBase64)
            })
        }
    }

    /**
     * 解析响应
     */
    private fun parseResponse(responseBody: String): Result<String> {
        return try {
            val json = JSONObject(responseBody)
            
            // 检查状态码
            val code = json.optInt("code", -1)
            if (code != 0) {
                val message = json.optString("message", "未知错误")
                return Result.failure(Exception("识别失败: $message (code: $code)"))
            }

            // 获取识别结果
            val result = json.optJSONObject("result")
            if (result != null) {
                val text = result.optString("text", "")
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("识别结果为空"))
                }
            } else {
                // 尝试其他格式
                val resp = json.optJSONObject("resp")
                if (resp != null) {
                    val text = resp.optString("text", "")
                    if (text.isNotBlank()) {
                        Result.success(text)
                    } else {
                        Result.failure(Exception("识别结果为空"))
                    }
                } else {
                    Result.failure(Exception("无法解析识别结果"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("解析响应失败: ${e.message}"))
        }
    }

    /**
     * 根据文件扩展名确定音频格式
     */
    private fun getAudioFormat(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "m4a" -> "m4a"
            "mp3" -> "mp3"
            "wav" -> "wav"
            "flac" -> "flac"
            "aac" -> "aac"
            "ogg" -> "ogg"
            "amr" -> "amr"
            else -> "m4a" // 默认格式
        }
    }
}
