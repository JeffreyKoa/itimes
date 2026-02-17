package com.xltool.quadrant.update

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import java.io.File

object DownloadReceiver {

    /**
     * 将 DownloadManager 的下载记录解析为我们期望的 apk 文件（位于 external files/Downloads 下）。
     * 说明：不同 ROM 可能返回 content://downloads/… 的 localUri；这里优先用我们写死的目标路径兜底。
     */
    fun resolveApkFile(context: Context, downloadId: Long): File? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = dm.query(query) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null

            val localUriIdx = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = if (statusIdx >= 0) it.getInt(statusIdx) else DownloadManager.STATUS_FAILED
            if (status != DownloadManager.STATUS_SUCCESSFUL) return null

            val localUri = if (localUriIdx >= 0) it.getString(localUriIdx) else null
            if (!localUri.isNullOrBlank()) {
                val uri = Uri.parse(localUri)
                if (uri.scheme == "file") {
                    return File(uri.path!!)
                }
            }
        }

        // 兜底：尝试在我们指定的目录中找最新的 xltool-*.apk
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        return dir.listFiles()
            ?.filter { f -> f.isFile && f.name.startsWith("xltool-") && f.name.endsWith(".apk") }
            ?.maxByOrNull { f -> f.lastModified() }
    }
}


