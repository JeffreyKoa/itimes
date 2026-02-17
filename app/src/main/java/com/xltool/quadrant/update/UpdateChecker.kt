package com.xltool.quadrant.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object UpdateChecker {

    fun hasUpdate(context: Context, info: UpdateInfo): Boolean {
        return info.versionCode > currentVersionCode(context)
    }

    fun currentVersionCode(context: Context): Int {
        val pm = context.packageManager
        val pkg = context.packageName
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pi = pm.getPackageInfo(pkg, 0)
                pi.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).versionCode
            }
        } catch (_: PackageManager.NameNotFoundException) {
            0
        }
    }

    /**
     * 简单的“启动时检查频率限制”：默认 6 小时检查一次，避免每次启动都打服务器。
     */
    fun shouldCheckNow(context: Context, cooldownMs: Long = 6 * 60 * 60 * 1000L): Boolean {
        val sp = context.getSharedPreferences("update", Context.MODE_PRIVATE)
        val last = sp.getLong("lastCheckAt", 0L)
        return (System.currentTimeMillis() - last) >= cooldownMs
    }

    fun markChecked(context: Context) {
        val sp = context.getSharedPreferences("update", Context.MODE_PRIVATE)
        sp.edit().putLong("lastCheckAt", System.currentTimeMillis()).apply()
    }
}


