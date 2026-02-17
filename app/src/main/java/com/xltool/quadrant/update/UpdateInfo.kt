package com.xltool.quadrant.update

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String?,
    val minSupportedCode: Int?
)


