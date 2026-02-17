package com.xltool.quadrant.update

import org.json.JSONObject
import java.net.URL
import java.nio.charset.Charset

object UpdateApi {

    /**
     * 拉取 update.json，返回服务器声明的最新版本信息。
     * 注意：这是“自建分发 APK”的更新方案；若走 Google Play，请改用 In-App Updates。
     */
    fun fetchUpdateInfo(updateJsonUrl: String): UpdateInfo {
        val jsonText = URL(updateJsonUrl).openStream().use { input ->
            input.readBytes().toString(Charset.forName("UTF-8"))
        }
        val obj = JSONObject(jsonText)
        return UpdateInfo(
            versionCode = obj.getInt("versionCode"),
            versionName = obj.optString("versionName", ""),
            apkUrl = obj.getString("apkUrl"),
            notes = obj.optString("notes").takeIf { it.isNotBlank() },
            minSupportedCode = obj.optInt("minSupportedCode").takeIf { obj.has("minSupportedCode") }
        )
    }
}


