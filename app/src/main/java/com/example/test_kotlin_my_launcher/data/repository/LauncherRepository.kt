package com.example.test_kotlin_my_launcher.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.test_kotlin_my_launcher.data.model.PageData
import com.example.test_kotlin_my_launcher.data.model.SavedPageData
import com.example.test_kotlin_my_launcher.ui.theme.DEFAULT_PAGE_COLOR

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_PAGE_COUNT = "page_count"
private const val KEY_PAGE_NAME = "page_name_"   // + index
private const val KEY_PAGE_APPS = "page_apps_"   // + index
private const val KEY_PAGE_COLOR = "page_color_"  // + index

fun loadSavedPages(context: Context): List<SavedPageData> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_PAGE_COUNT, 0)
    if (count == 0) return emptyList()
    return (0 until count).map { i ->
        val name = prefs.getString("$KEY_PAGE_NAME$i", "ページ ${i + 1}") ?: "ページ ${i + 1}"
        val raw = prefs.getString("$KEY_PAGE_APPS$i", "") ?: ""
        val apps = if (raw.isEmpty()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        val color = prefs.getLong("$KEY_PAGE_COLOR$i", DEFAULT_PAGE_COLOR)
        SavedPageData(name, apps, color)
    }
}

fun saveAllPages(context: Context, pages: List<PageData>) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_PAGE_COUNT, pages.size)
        pages.forEachIndexed { i, page ->
            putString("$KEY_PAGE_NAME$i", page.name)
            putString("$KEY_PAGE_APPS$i", page.apps.joinToString(",") { it.uniqueKey })
            putLong("$KEY_PAGE_COLOR$i", page.color)
        }
    }

fun clearAllSavedData(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
