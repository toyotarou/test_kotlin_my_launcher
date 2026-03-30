package com.example.test_kotlin_my_launcher.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.test_kotlin_my_launcher.ui.theme.DEFAULT_PAGE_COLOR

/** SharedPreferences から読み込んだ生データ（一時的なDTO） */
data class SavedPageData(val name: String, val appOrder: List<String>, val color: Long)

/** UI で直接使う状態付きのページモデル */
class PageData(name: String, color: Long = DEFAULT_PAGE_COLOR) {
    var name: String by mutableStateOf(name)
    var color: Long by mutableStateOf(color)
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()
}
