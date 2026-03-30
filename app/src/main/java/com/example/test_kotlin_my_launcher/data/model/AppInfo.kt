package com.example.test_kotlin_my_launcher.data.model

import android.graphics.Bitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val shortcutId: String? = null  // null = 通常アプリ, non-null = ピン留めショートカット
) {
    // ドラッグ追跡・グリッドキー用の一意キー
    val uniqueKey: String get() = if (shortcutId != null) "s:$packageName/$shortcutId" else packageName
}
