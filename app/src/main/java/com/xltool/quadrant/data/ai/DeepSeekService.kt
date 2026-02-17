package com.xltool.quadrant.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * DeepSeek AI 服务
 */
class DeepSeekService {

    companion object {
        private const val API_URL = "https://api.deepseek.com/chat/completions"
        private const val API_KEY = "sk-1f2a036f155b4fdf971bd1e5662e27b5"
        private const val MODEL = "deepseek-chat"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 使用火山引擎语音识别服务
    private val volcengineASR = VolcengineASRService()

    /**
     * 将音频文件转录为文字
     * 使用火山引擎(Volcengine)语音识别服务
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        return volcengineASR.transcribeAudio(audioFile)
    }

    /**
     * 从任务标题和描述中生成标签
     * @param title 任务标题
     * @param description 任务描述
     * @return 生成的标签列表，用逗号分隔
     */
    suspend fun generateTags(title: String, description: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(title, description)
                val response = callApi(prompt)
                Result.success(parseResponse(response))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseResponse(response: String): String {
        val jsonResponse = JSONObject(response)
        val choices = jsonResponse.getJSONArray("choices")
        if (choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content").trim()
            // 清理输出，确保只有标签
            return content
                .replace("标签：", "")
                .replace("标签:", "")
                .replace("，", ", ")
                .trim()
        }
        return ""
    }

    private fun buildPrompt(title: String, description: String): String {
        return """
你是一个任务标签生成助手。请根据以下任务信息，生成3-5个简短的中文标签，用于分类和快速识别任务。

任务标题：$title
任务描述：${description.ifBlank { "无" }}

要求：
1. 标签应该简短（2-4个字）
2. 标签应该能概括任务的类型、领域或特点
3. 只返回标签，用逗号分隔
4. 不要有其他解释文字

示例输出格式：工作, 紧急, 会议
        """.trimIndent()
    }

    /**
     * 分析任务所属象限
     * @param title 任务标题
     * @param description 任务描述
     * @return 象限ID (1-4)
     */
    suspend fun analyzeQuadrant(title: String, description: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildQuadrantPrompt(title, description)
                val response = callApi(prompt, "你是一个时间管理专家，擅长使用四象限法则（Eisenhower Matrix）对任务进行分类。")
                val quadrant = parseQuadrantResponse(response)
                Result.success(quadrant)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildQuadrantPrompt(title: String, description: String): String {
        return """
请根据以下任务信息，判断该任务属于四象限法则中的哪个象限。

任务标题：$title
任务描述：${description.ifBlank { "无" }}

四象限定义：
1. 重要且紧急（必须马上做，如：危机、截止日期临近的项目）
2. 重要不紧急（应该做，如：规划、学习、锻炼、建立关系）
3. 紧急不重要（不得不做，如：不速之客、某些会议、琐事）
4. 不重要不紧急（尽量不做，如：刷剧、发呆、浪费时间）

请只返回一个数字（1、2、3 或 4），代表所属象限。不要有任何解释或其他文字。
        """.trimIndent()
    }

    private fun parseQuadrantResponse(response: String): Int {
        val jsonResponse = JSONObject(response)
        val choices = jsonResponse.getJSONArray("choices")
        if (choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content").trim()
            // 提取数字
            val regex = """[1-4]""".toRegex()
            val match = regex.find(content)
            return match?.value?.toInt() ?: 2 // 默认重要不紧急
        }
        return 2
    }

    /**
     * 解析语音输入并分析象限
     * @param voiceText 语音转文字后的内容
     * @return 包含标题、描述、象限的 TaskDraft
     */
    suspend fun parseVoiceInput(voiceText: String): Result<ParsedTask> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildVoiceParsePrompt(voiceText)
                val response = callApi(prompt, "你是一个智能任务分析助手，能够从用户的语音文本中提取任务信息并进行分类。")
                val parsed = parseVoiceResponse(response)
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildVoiceParsePrompt(voiceText: String): String {
        return """
请分析以下语音输入的文本，提取任务标题、任务描述（如果有），并根据四象限法则判断所属象限。

语音内容：$voiceText

四象限定义：
1. 重要且紧急
2. 重要不紧急
3. 紧急不重要
4. 不重要不紧急

请以 JSON 格式返回结果，包含以下字段：
- title: 任务标题（简练概括）
- description: 任务详情（如果有额外信息）
- quadrant: 象限数字（1-4）

示例输出：
{
  "title": "购买牛奶",
  "description": "记得买全脂的",
  "quadrant": 3
}
        """.trimIndent()
    }

    private fun parseVoiceResponse(response: String): ParsedTask {
        val jsonResponse = JSONObject(response)
        val choices = jsonResponse.getJSONArray("choices")
        if (choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content").trim()
            
            // 尝试提取 JSON 部分
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}")
            
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonStr = content.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonStr)
                return ParsedTask(
                    title = json.optString("title", ""),
                    description = json.optString("description", ""),
                    quadrant = json.optInt("quadrant", 2)
                )
            }
        }
        return ParsedTask(title = "", description = "", quadrant = 2)
    }

    data class ParsedTask(
        val title: String,
        val description: String,
        val quadrant: Int
    )
    private fun callApi(prompt: String, systemPrompt: String? = null): String {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $API_KEY")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt ?: "你是一个专业的任务标签生成助手，擅长从任务描述中提取关键词作为标签。")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.3) // 降低温度以获得更确定的结果
                put("max_tokens", 100)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("API request failed: $responseCode - $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
}

