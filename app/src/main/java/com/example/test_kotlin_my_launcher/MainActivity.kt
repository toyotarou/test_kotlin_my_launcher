package com.example.test_kotlin_my_launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material3.AlertDialog
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
private const val KEY_PAGE_NAME = "page_name_"  // + index
private const val KEY_PAGE_APPS = "page_apps_"  // + index

private data class SavedPageData(val name: String, val appOrder: List<String>)

private fun loadSavedPages(context: Context): List<SavedPageData> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_PAGE_COUNT, 0)
    if (count == 0) return emptyList()
    return (0 until count).map { i ->
        val name = prefs.getString("$KEY_PAGE_NAME$i", "ページ ${i + 1}") ?: "ページ ${i + 1}"
        val raw = prefs.getString("$KEY_PAGE_APPS$i", "") ?: ""
        val apps = if (raw.isEmpty()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        SavedPageData(name, apps)
    }
}

private fun saveAllPages(context: Context, pages: List<PageData>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_PAGE_COUNT, pages.size)
        pages.forEachIndexed { i, page ->
            putString("$KEY_PAGE_NAME$i", page.name)
            putString("$KEY_PAGE_APPS$i", page.apps.joinToString(",") { it.packageName })
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
    val icon: Bitmap
)

class PageData(name: String) {
    var name: String by mutableStateOf(name)
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()
}

// ─────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // super より前に呼ぶ必要あり
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { LauncherScreen() } }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// LauncherScreen
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pages = remember { mutableStateListOf<PageData>() }
    var loadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(loadKey) {
        data class RawData(
            val allAppsMap: Map<String, AppInfo>,
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
            RawData(appsMap, loadSavedPages(context))
        }
        pages.clear()
        if (raw.savedPages.isEmpty()) {
            PageData("ページ 1").also { p ->
                p.apps.addAll(raw.allAppsMap.values.sortedBy { it.label })
                pages.add(p)
            }
        } else {
            val created = raw.savedPages.map { saved ->
                PageData(saved.name).also { p ->
                    saved.appOrder.forEach { pkg -> raw.allAppsMap[pkg]?.let { p.apps.add(it) } }
                }
            }
            val assigned = created.flatMap { it.apps.map { a -> a.packageName } }.toSet()
            raw.allAppsMap.values.filter { it.packageName !in assigned }.sortedBy { it.label }
                .let { created.lastOrNull()?.apps?.addAll(it) }
            pages.addAll(created)
        }
    }

    val pagerState = rememberPagerState { pages.size.coerceAtLeast(1) }

    var isEditMode by remember { mutableStateOf(false) }
    // ドラッグ中アプリと元ページ（最上位 Box で管理するためページ切り替え時にキャンセルされない）
    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var draggingFromPage by remember { mutableStateOf<Int?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var showResetDialog by remember { mutableStateOf(false) }
    var deletePageTarget by remember { mutableStateOf<Int?>(null) }
    var editingPageName by remember { mutableStateOf(false) }
    var pageNameInput by remember { mutableStateOf("") }
    var lastPageScrollMs by remember { mutableLongStateOf(0L) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val statusBarPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(editingPageName) { if (editingPageName) focusRequester.requestFocus() }

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
            onDismissRequest = { },
            title = { Text("パネルをリセット") },
            text = {
                Text(
                    "・追加したページをすべて削除\n" +
                            "・ページ名を初期状態に戻す\n" +
                            "・アイコンの並び順をアルファベット順に戻す\n\n" +
                            "よろしいですか？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    isEditMode = false
                    editingPageName = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) { clearAllSavedData(context) }
                        loadKey++
                    }
                }) { Text("リセット", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("キャンセル") }
            }
        )
    }

    // ── ページ削除確認ダイアログ ─────────────────────────────────────────
    deletePageTarget?.let { targetIdx ->
        AlertDialog(
            onDismissRequest = { deletePageTarget = null },
            title = { Text("ページを削除") },
            text = {
                val name = pages.getOrNull(targetIdx)?.name ?: ""
                Text("「$name」を削除しますか？\nこのページのアプリはすべて最初のページへ移動します。")
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
                TextButton(onClick = { deletePageTarget = null }) { Text("キャンセル") }
            }
        )
    }

    // ── 画面全体
    // ★ ドラッグ検出を最上位 Box に置く。
    //    HorizontalPager のページアニメーションが起きてもここのジェスチャーは絶対にキャンセルされない。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // 押した座標に対応するタイルを boundsInRoot で探す
                        val hit = itemBoundsMap.entries.firstOrNull { (_, bounds) ->
                            bounds.contains(offset)
                        } ?: return@detectDragGesturesAfterLongPress
                        val fromPage =
                            pages.indexOfFirst { p -> p.apps.any { it.packageName == hit.key } }
                        if (fromPage < 0) return@detectDragGesturesAfterLongPress
                        draggingPkg = hit.key
                        draggingFromPage = fromPage
                        dragPosition = offset
                        isEditMode = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val curPkg = draggingPkg ?: return@detectDragGesturesAfterLongPress
                        dragPosition += dragAmount

                        // ページエッジスクロール（左右端 80px で自動ページ切り替え）
                        val now = System.currentTimeMillis()
                        if (now - lastPageScrollMs > 700L) {
                            val cp = pagerState.currentPage
                            when {
                                dragPosition.x < 80f && cp > 0 -> {
                                    coroutineScope.launch { pagerState.animateScrollToPage(cp - 1) }
                                }

                                dragPosition.x > screenWidthPx - 80f && cp < pages.size - 1 -> {
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
                                                pageApps.any { it.packageName == pkg }
                                    }
                                    ?.key
                                    ?.let { hoverPkg ->
                                        val fi = pageApps.indexOfFirst { it.packageName == curPkg }
                                        val ti =
                                            pageApps.indexOfFirst { it.packageName == hoverPkg }
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
                        val toPage = pagerState.currentPage
                        // 別ページへのドロップ → アプリを移動
                        if (curPkg != null && fromPage != null && fromPage != toPage) {
                            val src = pages.getOrNull(fromPage)
                            val dst = pages.getOrNull(toPage)
                            if (src != null && dst != null) {
                                src.apps.firstOrNull { it.packageName == curPkg }?.let { app ->
                                    src.apps.remove(app)
                                    dst.apps.add(app)
                                }
                            }
                        }
                        draggingPkg = null
                        draggingFromPage = null
                        dragPosition = Offset.Zero
                        saveCurrent()
                    },
                    onDragCancel = {
                        draggingPkg = null
                        draggingFromPage = null
                        dragPosition = Offset.Zero
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarPad + 52.dp))

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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp
                        )
                    ) {
                        itemsIndexed(
                            items = page.apps,
                            key = { _, app -> app.packageName }
                        ) { _, app ->
                            AppTile(
                                app = app,
                                isDragging = draggingPkg == app.packageName,
                                isEditMode = isEditMode,
                                onDelete = { page.apps.remove(app); saveCurrent() },
                                onTap = {
                                    if (isEditMode) {
                                        isEditMode = false
                                    } else {
                                        context.packageManager
                                            .getLaunchIntentForPackage(app.packageName)
                                            ?.let { context.startActivity(it) }
                                    }
                                },
                                // bounds 追跡のみ。ドラッグジェスチャーは最上位 Box が担当
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    itemBoundsMap[app.packageName] = coords.boundsInRoot()
                                }
                            )
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

            // ── ページインジケーター ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = navBarPad + 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val isCurrent = pagerState.currentPage == index
                    Box(modifier = Modifier.padding(horizontal = 5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 10.dp else 7.dp)
                                .background(
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                        )
                        if (isEditMode && pages.size > 1) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 5.dp, y = (-5).dp)
                                    .background(Color(0xFFD32F2F), CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                                    .clickable { deletePageTarget = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", color = Color.White, fontSize = 7.sp, lineHeight = 7.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.20f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .clickable {
                            val newPage = PageData("ページ ${pages.size + 1}")
                            pages.add(newPage)
                            val newIdx = pages.size - 1
                            coroutineScope.launch { pagerState.animateScrollToPage(newIdx) }
                            saveCurrent()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (editingPageName && pages.isNotEmpty()) {
                        BasicTextField(
                            value = pageNameInput,
                            onValueChange = { pageNameInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val t = pageNameInput.trim()
                                if (t.isNotEmpty()) {
                                    pages[pagerState.currentPage].name = t
                                    saveCurrent()
                                }
                                editingPageName = false
                            }),
                            decorationBox = { inner ->
                                Box {
                                    if (pageNameInput.isEmpty()) {
                                        Text(
                                            "ページ名を入力",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 15.sp
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    } else {
                        Column {
                            val currentName = pages.getOrNull(pagerState.currentPage)?.name ?: ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        if (pages.isNotEmpty()) {
                                            pageNameInput = pages[pagerState.currentPage].name
                                            editingPageName = true
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✎",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable {
                                        if (pages.isNotEmpty()) {
                                            pageNameInput = pages[pagerState.currentPage].name
                                            editingPageName = true
                                        }
                                    }
                                )
                            }
                            if (isEditMode) {
                                Text(
                                    text = "✕タップで削除 / タイルタップで終了",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showResetDialog = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "リセット",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .clickable { isEditMode = false }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "完了",
                                color = Color(0xFF1A1A2E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── ドラッグゴースト ─────────────────────────────────────────────
        if (draggingPkg != null) {
            val draggedApp = pages.flatMap { it.apps }.firstOrNull { it.packageName == draggingPkg }
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
    modifier: Modifier = Modifier
) {
    val bitmap = remember(app.packageName) { app.icon.asImageBitmap() }

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
        }
    }
}
