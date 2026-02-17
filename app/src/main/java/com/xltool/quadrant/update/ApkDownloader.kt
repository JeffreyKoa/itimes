package com.xltool.quadrant.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object ApkDownloader {

    fun enqueue(context: Context, apkUrl: String, versionName: String): Long {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("下载更新 ${versionName}")
            .setDescription("象限任务管理器正在下载新版本…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            // 下载到应用专属外部目录，避免需要额外存储权限
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "xltool-${versionName}.apk"
            )

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}


