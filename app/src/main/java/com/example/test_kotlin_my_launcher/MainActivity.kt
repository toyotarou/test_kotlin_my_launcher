package com.example.test_kotlin_my_launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────
// SharedPreferences helpers
// ─────────────────────────────────────────────────────────────────────────
private const val PREFS_NAME = "launcher_prefs"
private const val KEY_PAGE_COUNT = "page_count"
private const val KEY_PAGE_NAME = "page_name_"   // + index
private const val KEY_PAGE_APPS = "page_apps_"   // + index
private const val KEY_PAGE_COLOR = "page_color_" // + index
private val DEFAULT_PAGE_COLOR = 0xFF3D5A8AL     // デフォルト: ダークブルー

// ── テーマカラーパレット（48色）
val themeColorPalette: List<Long> = listOf(
    // 深色系
    0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFF8E24AAL, 0xFFFFA726L, 0xFF00ACC1L,
    0xFFFDD835L, 0xFF6D4C41L, 0xFFD81B60L, 0xFF3949ABL, 0xFF00897BL, 0xFF7CB342L,
    0xFF5E35B1L, 0xFFFB8C00L, 0xFF00838FL, 0xFFF4511EL, 0xFF558B2FL, 0xFF6A1B9AL,
    0xFF2E7D32L, 0xFF283593L, 0xFFAD1457L, 0xFF4E342EL, 0xFF1565C0L, 0xFF9E9D24L,
    // 中明度系
    0xFF42A5F5L, 0xFF66BB6AL, 0xFFAB47BCL, 0xFFFFB74DL, 0xFF26C6DAL, 0xFFFFF176L,
    0xFF8D6E63L, 0xFFF06292L, 0xFF5C6BC0L, 0xFF26A69AL, 0xFF9CCC65L, 0xFF9575CDL,
    // 淡色系
    0xFFFFCC80L, 0xFF80DEEAL, 0xFFFFAB91L, 0xFFC5E1A5L, 0xFFB39DDBL, 0xFFA5D6A7L,
    0xFF9FA8DAL, 0xFFF48FB1L, 0xFFBCAAA4L, 0xFFEF5350L, 0xFFBDBDBDL, 0xFFE0E0E0L
)

// 選択カラーから暗めのグラデーション3色を生成
fun themeGradient(base: Color): List<Color> {
    fun dk(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f)
    return listOf(dk(base, 0.30f), dk(base, 0.52f), dk(base, 0.78f))
}

private data class SavedPageData(val name: String, val appOrder: List<String>, val color: Long)

private fun loadSavedPages(context: Context): List<SavedPageData> {
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

private fun saveAllPages(context: Context, pages: List<PageData>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_PAGE_COUNT, pages.size)
        pages.forEachIndexed { i, page ->
            putString("$KEY_PAGE_NAME$i", page.name)
            putString("$KEY_PAGE_APPS$i", page.apps.joinToString(",") { it.uniqueKey })
            putLong("$KEY_PAGE_COLOR$i", page.color)
        }
    }
}

private fun clearAllSavedData(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
}

// ─────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val shortcutId: String? = null  // null = 通常アプリ, non-null = ピン留めショートカット
) {
    // ドラッグ追跡・グリッドキー用の一意キー
    val uniqueKey: String get() = if (shortcutId != null) "s:$packageName/$shortcutId" else packageName
}

class PageData(name: String, color: Long = DEFAULT_PAGE_COLOR) {
    var name: String by mutableStateOf(name)
    var color: Long by mutableStateOf(color)
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()
}

// ─────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    private val _pinTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // super より前に呼ぶ必要あり
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptPinShortcut(intent)
        setContent { MaterialTheme { LauncherScreen(pinTrigger = _pinTrigger.intValue) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptPinShortcut(intent)
    }

    // Chrome などから「ホーム画面に追加」されたときに呼ばれる
    private fun acceptPinShortcut(intent: Intent?) {
        if (intent?.action != "android.content.pm.action.CONFIRM_PIN_SHORTCUT") return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val la = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            la.getPinItemRequest(intent)
                ?.takeIf { it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT }
                ?.accept()
        }
        _pinTrigger.intValue++
    }
}

// ─────────────────────────────────────────────────────────────────────────
// LauncherScreen
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun LauncherScreen(pinTrigger: Int = 0) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pages = remember { mutableStateListOf<PageData>() }
    var loadKey by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Chrome などでショートカットがピン留めされたら再読み込み
    LaunchedEffect(pinTrigger) { if (pinTrigger > 0) loadKey++ }

    LaunchedEffect(loadKey) {
        data class RawData(
            val allAppsMap: Map<String, AppInfo>,       // key: packageName
            val shortcutsMap: Map<String, AppInfo>,     // key: uniqueKey ("s:pkg/id")
            val savedPages: List<SavedPageData>
        )

        val raw = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent =
                Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val appsMap = pm.queryIntentActivities(intent, 0)
                .distinctBy { it.activityInfo.packageName }
                .mapNotNull { info ->
                    try {
                        AppInfo(
                            packageName = info.activityInfo.packageName,
                            label = info.loadLabel(pm).toString(),
                            icon = info.loadIcon(pm).toBitmap(96, 96)
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .associateBy { it.packageName }

            // ピン留めショートカット（API 25+ / Chrome「ホーム画面に追加」など）
            val shortcutsMap: Map<String, AppInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                try {
                    val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

                    // デフォルトホームアプリでないと getShortcuts() は SecurityException になる
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !la.hasShortcutHostPermission()) {
                        emptyMap()
                    } else {
                        // API 29+: 他のランチャーでピン留めされたものも含めて取得
                        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED_BY_ANY_LAUNCHER
                        } else {
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                        }
                        val query = LauncherApps.ShortcutQuery().setQueryFlags(flags)

                        @Suppress("NewApi")
                        la.getShortcuts(query, Process.myUserHandle())
                            ?.mapNotNull { info ->
                                val drawable = try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        la.getShortcutBadgedIconDrawable(info, 0)
                                            ?: la.getShortcutIconDrawable(info, 0)
                                    } else {
                                        la.getShortcutIconDrawable(info, 0)
                                    }
                                } catch (_: Exception) { null }
                                val bitmap = if (drawable != null) {
                                    drawable.toBitmap(96, 96)
                                } else {
                                    android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888).also { bmp ->
                                        val canvas = android.graphics.Canvas(bmp)
                                        android.graphics.Paint().also { paint ->
                                            paint.color = android.graphics.Color.GRAY
                                            canvas.drawCircle(48f, 48f, 48f, paint)
                                        }
                                    }
                                }
                                AppInfo(
                                    packageName = info.`package`,
                                    label = info.shortLabel?.toString() ?: info.`package`,
                                    icon = bitmap,
                                    shortcutId = info.id
                                )
                            }
                            ?.associateBy { it.uniqueKey }
                            ?: emptyMap()
                    }
                } catch (_: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            RawData(appsMap, shortcutsMap, loadSavedPages(context))
        }
        pages.clear()
        if (raw.savedPages.isEmpty()) {
            PageData("ページ 1").also { p ->
                p.apps.addAll(raw.allAppsMap.values.sortedBy { it.label })
                p.apps.addAll(raw.shortcutsMap.values.sortedBy { it.label })
                pages.add(p)
            }
        } else {
            val created = raw.savedPages.map { saved ->
                PageData(saved.name, saved.color).also { p ->
                    saved.appOrder.forEach { key ->
                        if (key.startsWith("s:")) {
                            raw.shortcutsMap[key]?.let { p.apps.add(it) }
                        } else {
                            raw.allAppsMap[key]?.let { p.apps.add(it) }
                        }
                    }
                }
            }
            val assignedKeys = created.flatMap { it.apps.map { a -> a.uniqueKey } }.toSet()
            raw.allAppsMap.values.filter { it.uniqueKey !in assignedKeys }.sortedBy { it.label }
                .let { created.firstOrNull()?.apps?.addAll(it) }
            raw.shortcutsMap.values.filter { it.uniqueKey !in assignedKeys }.sortedBy { it.label }
                .let { created.firstOrNull()?.apps?.addAll(it) }
            pages.addAll(created)
        }
        isRefreshing = false
    }

    val pagerState = rememberPagerState { pages.size.coerceAtLeast(1) }

    var isEditMode by remember { mutableStateOf(false) }
    // ドラッグ中アプリと元ページ（最上位 Box で管理するためページ切り替え時にキャンセルされない）
    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var draggingFromPage by remember { mutableStateOf<Int?>(null) }
    var dragTargetPage by remember { mutableStateOf<Int?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var showResetDialog by remember { mutableStateOf(false) }
    var deletePageTarget by remember { mutableStateOf<Int?>(null) }
    var editingPageName by remember { mutableStateOf(false) }
    var pageNameInput by remember { mutableStateOf("") }
    var lastPageScrollMs by remember { mutableLongStateOf(0L) }
    var showColorPicker by remember { mutableStateOf(false) }
    var isTabMoveMode by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val statusBarPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tabListState = rememberLazyListState()
    LaunchedEffect(editingPageName) { if (editingPageName) focusRequester.requestFocus() }
    LaunchedEffect(pagerState.currentPage) {
        if (pages.isNotEmpty()) tabListState.animateScrollToItem(pagerState.currentPage)
    }

    fun saveCurrent() {
        coroutineScope.launch(Dispatchers.IO) { saveAllPages(context, pages) }
    }

    BackHandler(enabled = isEditMode || editingPageName) {
        if (editingPageName) {
            keyboardController?.hide()
            editingPageName = false
        } else {
            isEditMode = false
        }
    }

    // ── リセット確認ダイアログ ──────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("リセット") },
            text = { Text("リセットして良いですか？") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    isEditMode = false
                    editingPageName = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) { clearAllSavedData(context) }
                        loadKey++
                    }
                }) { Text("はい", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("いいえ") }
            }
        )
    }

    // ── ページ削除確認ダイアログ ─────────────────────────────────────────
    deletePageTarget?.let { targetIdx ->
        AlertDialog(
            onDismissRequest = { deletePageTarget = null },
            title = { Text("ページを削除") },
            text = {
                val appCount = pages.getOrNull(targetIdx)?.apps?.size ?: 0
                val name = pages.getOrNull(targetIdx)?.name ?: ""
                if (appCount > 0) {
                    Text("アプリアイコンが存在しますがこのタブを消して良いですか？\n（${appCount}個のアプリは最初のページへ移動します）")
                } else {
                    Text("「$name」を削除しますか？")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pages.size > 1) {
                        val removed = pages.removeAt(targetIdx)
                        pages.firstOrNull()?.apps?.addAll(removed.apps)
                        val safePage = (targetIdx - 1).coerceAtLeast(0)
                        coroutineScope.launch { pagerState.animateScrollToPage(safePage) }
                        saveCurrent()
                    }
                    deletePageTarget = null
                }) { Text("削除", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deletePageTarget = null }) { Text("いいえ") }
            }
        )
    }

    // ── カラーピッカーダイアログ ──────────────────────────────────────────
    if (showColorPicker) {
        val currentPage = pages.getOrNull(pagerState.currentPage)
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("「${currentPage?.name ?: ""}」の背景色") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    themeColorPalette.chunked(6).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowColors.forEach { colorLong ->
                                val isSelected = currentPage?.color == colorLong
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(colorLong.toInt()), CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White
                                            else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            currentPage?.color = colorLong
                                            saveCurrent()
                                            showColorPicker = false
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("閉じる") }
            }
        )
    }

    // ── 画面全体
    // ★ ドラッグ検出を最上位 Box に置く。
    //    HorizontalPager のページアニメーションが起きてもここのジェスチャーは絶対にキャンセルされない。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A.toInt()))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // 現在表示中のページのアプリのみを対象にして座標ヒットを検索する。
                        // HorizontalPager は隣接ページも事前描画するため itemBoundsMap には
                        // 全ページの bounds が混在する。現ページ外の stale bounds を踏まないよう
                        // 現ページの packageName セットでフィルタする。
                        val currentPageIdx = pagerState.currentPage
                        val currentPagePkgs = pages.getOrNull(currentPageIdx)
                            ?.apps?.map { it.uniqueKey }?.toSet() ?: emptySet()
                        val hit = itemBoundsMap.entries.firstOrNull { (pkg, bounds) ->
                            pkg in currentPagePkgs && bounds.contains(offset)
                        } ?: return@detectDragGesturesAfterLongPress
                        draggingPkg = hit.key
                        draggingFromPage = currentPageIdx
                        dragTargetPage = null
                        dragPosition = offset
                        isEditMode = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val curPkg = draggingPkg ?: return@detectDragGesturesAfterLongPress
                        dragPosition += dragAmount

                        // ページエッジスクロール（左右端 80px で自動ページ切り替え）
                        val now = System.currentTimeMillis()
                        if (now - lastPageScrollMs > 1200L) {
                            val cp = pagerState.currentPage
                            when {
                                dragPosition.x < 80f && cp > 0 -> {
                                    lastPageScrollMs = now
                                    dragTargetPage = cp - 1
                                    coroutineScope.launch { pagerState.animateScrollToPage(cp - 1) }
                                }

                                dragPosition.x > screenWidthPx - 80f && cp < pages.size - 1 -> {
                                    lastPageScrollMs = now
                                    dragTargetPage = cp + 1
                                    coroutineScope.launch { pagerState.animateScrollToPage(cp + 1) }
                                }
                            }
                        }

                        // 同ページ内の並び替え（ドラッグ元と現在表示ページが同じ場合のみ）
                        if (pagerState.currentPage == draggingFromPage) {
                            val pageApps = pages.getOrNull(pagerState.currentPage)?.apps
                            if (pageApps != null) {
                                itemBoundsMap.entries
                                    .firstOrNull { (pkg, bounds) ->
                                        pkg != curPkg &&
                                                bounds.contains(dragPosition) &&
                                                pageApps.any { it.uniqueKey == pkg }
                                    }
                                    ?.key
                                    ?.let { hoverPkg ->
                                        val fi = pageApps.indexOfFirst { it.uniqueKey == curPkg }
                                        val ti =
                                            pageApps.indexOfFirst { it.uniqueKey == hoverPkg }
                                        if (fi >= 0 && ti >= 0) pageApps.add(
                                            ti,
                                            pageApps.removeAt(fi)
                                        )
                                    }
                            }
                        }
                    },
                    onDragEnd = {
                        val curPkg = draggingPkg
                        val fromPage = draggingFromPage
                        // アニメーション未完了でも意図したページへ移動できるよう dragTargetPage を優先
                        val toPage = dragTargetPage ?: pagerState.currentPage
                        // 別ページへのドロップ → アプリを移動
                        if (curPkg != null && fromPage != null && fromPage != toPage) {
                            val src = pages.getOrNull(fromPage)
                            val dst = pages.getOrNull(toPage)
                            if (src != null && dst != null) {
                                src.apps.firstOrNull { it.uniqueKey == curPkg }?.let { app ->
                                    src.apps.remove(app)
                                    dst.apps.add(app)
                                }
                            }
                        }
                        draggingPkg = null
                        draggingFromPage = null
                        dragTargetPage = null
                        dragPosition = Offset.Zero
                        saveCurrent()
                    },
                    onDragCancel = {
                        draggingPkg = null
                        draggingFromPage = null
                        dragTargetPage = null
                        dragPosition = Offset.Zero
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarPad + 140.dp))

            // ── HorizontalPager
            // ★ ドラッグ中はユーザーによるスワイプを無効化。
            //    ページ切り替えは上の pointerInput から animateScrollToPage で行う。
            if (pages.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = draggingPkg == null,  // ドラッグ中はページスワイプを封じる
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = themeGradient(Color(page.color.toInt()).copy(alpha = 1f))
                                )
                            )
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp
                            )
                        ) {
                            itemsIndexed(
                                items = page.apps,
                                key = { _, app -> app.uniqueKey }
                            ) { _, app ->  // pageIndex は HorizontalPager のスコープ変数を使う
                                AppTile(
                                    app = app,
                                    isDragging = draggingPkg == app.uniqueKey,
                                    isEditMode = isEditMode,
                                    onDelete = { page.apps.remove(app); saveCurrent() },
                                    onMoveToFirst = if (pageIndex > 0) {
                                        {
                                            page.apps.remove(app)
                                            pages.firstOrNull()?.apps?.add(app)
                                            saveCurrent()
                                        }
                                    } else null,
                                    onTap = {
                                        if (isEditMode) {
                                            isEditMode = false
                                        } else if (app.shortcutId != null) {
                                            // ピン留めショートカット（Chrome ブックマークなど）を起動
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                                try {
                                                    val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                                                    la.startShortcut(app.packageName, app.shortcutId, null, null, Process.myUserHandle())
                                                } catch (_: Exception) {}
                                            }
                                        } else {
                                            context.packageManager
                                                .getLaunchIntentForPackage(app.packageName)
                                                ?.let { context.startActivity(it) }
                                        }
                                    },
                                    // bounds 追跡のみ。ドラッグジェスチャーは最上位 Box が担当
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        itemBoundsMap[app.uniqueKey] = coords.boundsInRoot()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("アプリを読み込み中...", color = Color.White, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(navBarPad + 8.dp))
        }

        // ── ページ名編集中: 枠外タップでキーボードを閉じるオーバーレイ ───
        if (editingPageName) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            keyboardController?.hide()
                            editingPageName = false
                        }
                    }
            )
        }

        // ── トップバー ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPad)
                .background(Color.Black.copy(alpha = if (isEditMode) 0.70f else 0.35f))
                .align(Alignment.TopCenter)
        ) {
            Column {
                // ── タイトル行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "あめらん",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "- Amaging Launcher -",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable(enabled = !isRefreshing) {
                                isRefreshing = true
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                    loadKey++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "リロード",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // ── アクション行（ボタン類）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── 左グループ: リセット ＋ タブ移動ボタン
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { showResetDialog = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("リセット", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // タブを掴む（トグル）
                    Box(
                        modifier = Modifier
                            .background(
                                if (isTabMoveMode) Color.White else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { isTabMoveMode = !isTabMoveMode }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isTabMoveMode) "タブを離す" else "タブを掴む",
                            color = if (isTabMoveMode) Color(0xFF1A1A2E) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // ◀ 左へ移動
                    val canMoveLeft = isTabMoveMode && pagerState.currentPage > 0
                    Box(
                        modifier = Modifier
                            .background(
                                if (canMoveLeft) Color.White.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (canMoveLeft) Color.White.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(enabled = canMoveLeft) {
                                val idx = pagerState.currentPage
                                val tab = pages.removeAt(idx)
                                pages.add(idx - 1, tab)
                                coroutineScope.launch { pagerState.animateScrollToPage(idx - 1) }
                                saveCurrent()
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "◀",
                            color = if (canMoveLeft) Color.White else Color.White.copy(alpha = 0.20f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // ▶ 右へ移動
                    val canMoveRight = isTabMoveMode && pagerState.currentPage < pages.size - 1
                    Box(
                        modifier = Modifier
                            .background(
                                if (canMoveRight) Color.White.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (canMoveRight) Color.White.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(enabled = canMoveRight) {
                                val idx = pagerState.currentPage
                                val tab = pages.removeAt(idx)
                                pages.add(idx + 1, tab)
                                coroutineScope.launch { pagerState.animateScrollToPage(idx + 1) }
                                saveCurrent()
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "▶",
                            color = if (canMoveRight) Color.White else Color.White.copy(alpha = 0.20f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ── 右グループ: 編集モード時のヒント＋完了 ＋ カラーパレット
                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .clickable { isEditMode = false }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("完了", color = Color(0xFF1A1A2E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF8B2020.toInt()), Color(0xFF8B6914.toInt()),
                                        Color(0xFF27AE60.toInt()), Color(0xFF1E6091.toInt()),
                                        Color(0xFF5E35B1.toInt()), Color(0xFF8B2020.toInt())
                                    )
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { showColorPicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // ── タブ行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                LazyRow(
                    state = tabListState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pages.size, key = { it }) { index ->
                        val page = pages[index]
                        val isActive = pagerState.currentPage == index
                        val pageColor = Color(page.color.toInt())
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isActive) pageColor.copy(alpha = 0.85f)
                                    else pageColor.copy(alpha = 0.35f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isActive) Color.White.copy(alpha = 0.60f)
                                    else Color.White.copy(alpha = 0.20f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isActive && !editingPageName) {
                                        // アクティブなタブをタップ → ページ名を編集
                                        pageNameInput = page.name
                                        editingPageName = true
                                    } else if (!isActive) {
                                        // 別タブをタップ → ページ切り替え（編集中なら破棄）
                                        keyboardController?.hide()
                                        editingPageName = false
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            if (isActive && editingPageName) {
                                BasicTextField(
                                    value = pageNameInput,
                                    onValueChange = { pageNameInput = it },
                                    modifier = Modifier.focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val t = pageNameInput.trim()
                                        if (t.isNotEmpty()) {
                                            pages[pagerState.currentPage].name = t
                                            saveCurrent()
                                        }
                                        keyboardController?.hide()
                                        editingPageName = false
                                    }),
                                    decorationBox = { inner ->
                                        Box {
                                            if (pageNameInput.isEmpty()) {
                                                Text(
                                                    "ページ名を入力",
                                                    color = Color.White.copy(alpha = 0.45f),
                                                    fontSize = 13.sp
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                            } else {
                                Text(
                                    text = page.name,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 編集モード時の × バッジ
                            if (isEditMode && pages.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-8).dp)
                                        .zIndex(10f)
                                        .background(Color(0xFFD32F2F), CircleShape)
                                        .border(1.dp, Color.White, CircleShape)
                                        .clickable { deletePageTarget = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = Color.White, fontSize = 8.sp, lineHeight = 8.sp)
                                }
                            }
                        }
                    }

                }

                    Spacer(modifier = Modifier.width(6.dp))

                    // ── ページ追加ボタン（+）
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable {
                                val newPage = PageData("ページ ${pages.size + 1}")
                                pages.add(newPage)
                                val newIdx = pages.size - 1
                                coroutineScope.launch { pagerState.animateScrollToPage(newIdx) }
                                saveCurrent()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // ── ページ削除ボタン（-）
                    val canDelete = pages.size > 1
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (canDelete) Color.White.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (canDelete) Color.White.copy(alpha = 0.4f)
                                else Color.White.copy(alpha = 0.10f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = canDelete) {
                                val currentIdx = pagerState.currentPage
                                val currentApps = pages.getOrNull(currentIdx)?.apps
                                if (currentApps.isNullOrEmpty()) {
                                    // アプリなし → 確認なしで即削除
                                    pages.removeAt(currentIdx)
                                    val safePage = (currentIdx - 1).coerceAtLeast(0)
                                    coroutineScope.launch { pagerState.animateScrollToPage(safePage) }
                                    saveCurrent()
                                } else {
                                    // アプリあり → 確認ダイアログ
                                    deletePageTarget = currentIdx
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "−",
                            color = if (canDelete) Color.White else Color.White.copy(alpha = 0.20f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── ドラッグゴースト ─────────────────────────────────────────────
        if (draggingPkg != null) {
            val draggedApp = pages.flatMap { it.apps }.firstOrNull { it.uniqueKey == draggingPkg }
            if (draggedApp != null) {
                val ghostSize = 80.dp
                Column(
                    modifier = Modifier
                        .size(ghostSize)
                        .offset {
                            IntOffset(
                                (dragPosition.x - ghostSize.toPx() / 2).roundToInt(),
                                (dragPosition.y - ghostSize.toPx() / 2).roundToInt()
                            )
                        }
                        .zIndex(100f)
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(18.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = draggedApp.icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// AppTile  ※ ドラッグジェスチャーは除去済み（最上位 Box が担当）
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun AppTile(
    app: AppInfo,
    isDragging: Boolean,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onTap: () -> Unit,
    onMoveToFirst: (() -> Unit)? = null,  // null = ページ1にいる場合は非表示
    modifier: Modifier = Modifier
) {
    val bitmap = remember(app.uniqueKey) { app.icon.asImageBitmap() }

    Box(modifier = modifier.padding(6.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDragging) {
                            listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.02f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.09f))
                        }
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 0.8.dp,
                    color = if (isDragging) Color.White.copy(alpha = 0.08f)
                    else Color.White.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { onTap() }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDragging) Color.White.copy(alpha = 0.35f) else Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isEditMode && !isDragging) {
            // 右上: 削除バッジ
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .zIndex(10f)
                    .background(Color(0xFFD32F2F), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
            // 左上: ページ1へ戻すバッジ（ページ1以外にいるときのみ表示）
            if (onMoveToFirst != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-3).dp, y = (-3).dp)
                        .zIndex(10f)
                        .background(Color(0xFF1565C0), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable(onClick = onMoveToFirst),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↩",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}
