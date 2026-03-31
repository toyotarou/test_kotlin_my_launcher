package com.example.test_kotlin_my_launcher.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import android.content.pm.LauncherApps
import androidx.compose.ui.focus.FocusRequester
import com.example.test_kotlin_my_launcher.data.model.AppInfo
import com.example.test_kotlin_my_launcher.data.model.PageData
import com.example.test_kotlin_my_launcher.data.repository.clearAllSavedData
import com.example.test_kotlin_my_launcher.data.repository.loadSavedPages
import com.example.test_kotlin_my_launcher.data.repository.saveAllPages
import com.example.test_kotlin_my_launcher.ui.component.AppTile
import com.example.test_kotlin_my_launcher.ui.component.DragGhost
import com.example.test_kotlin_my_launcher.ui.component.TopBar
import com.example.test_kotlin_my_launcher.ui.dialog.ColorPickerDialog
import com.example.test_kotlin_my_launcher.ui.dialog.DeletePageDialog
import com.example.test_kotlin_my_launcher.ui.dialog.ResetDialog
import com.example.test_kotlin_my_launcher.ui.dialog.SearchNotFoundDialog
import com.example.test_kotlin_my_launcher.ui.dialog.SearchResult
import com.example.test_kotlin_my_launcher.ui.dialog.SearchResultsDialog
import com.example.test_kotlin_my_launcher.ui.theme.themeGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val allAppsMap: Map<String, AppInfo>,
            val shortcutsMap: Map<String, AppInfo>,
            val savedPages: List<com.example.test_kotlin_my_launcher.data.model.SavedPageData>
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
                    } catch (_: Exception) { null }
                }
                .associateBy { it.packageName }

            val shortcutsMap: Map<String, AppInfo> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    try {
                        val la =
                            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !la.hasShortcutHostPermission()) {
                            emptyMap()
                        } else {
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
                                        android.graphics.Bitmap.createBitmap(
                                            96, 96, android.graphics.Bitmap.Config.ARGB_8888
                                        ).also { bmp ->
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
                    } catch (_: Exception) { emptyMap() }
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
                        if (key.startsWith("s:")) raw.shortcutsMap[key]?.let { p.apps.add(it) }
                        else raw.allAppsMap[key]?.let { p.apps.add(it) }
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
    var searchQuery by remember { mutableStateOf("") }
    var showSearchNotFound by remember { mutableStateOf(false) }
    var searchLastQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    // (pageIndex, itemIndex) — ページ遷移後にグリッドスクロールするターゲット
    var searchScrollTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

    fun onSearchSubmit() {
        val query = searchQuery.trim()
        searchQuery = ""
        keyboardController?.hide()
        if (query.isBlank()) return
        val hits = mutableListOf<SearchResult>()
        for ((pageIdx, page) in pages.withIndex()) {
            page.apps.forEachIndexed { appIdx, app ->
                if (app.label.contains(query, ignoreCase = true)) {
                    hits.add(SearchResult(pageIdx, page.name, appIdx, app))
                }
            }
        }
        if (hits.isEmpty()) {
            searchLastQuery = query
            showSearchNotFound = true
        } else {
            searchLastQuery = query
            searchResults = hits
            showSearchResults = true
        }
    }

    BackHandler(enabled = isEditMode || editingPageName) {
        if (editingPageName) {
            keyboardController?.hide()
            editingPageName = false
        } else {
            isEditMode = false
        }
    }

    // ── ダイアログ ─────────────────────────────────────────────────────────
    if (showResetDialog) {
        ResetDialog(
            onConfirm = {
                showResetDialog = false
                isEditMode = false
                editingPageName = false
                coroutineScope.launch {
                    withContext(Dispatchers.IO) { clearAllSavedData(context) }
                    loadKey++
                }
            },
            onDismiss = { showResetDialog = false }
        )
    }

    deletePageTarget?.let { targetIdx ->
        DeletePageDialog(
            pageName = pages.getOrNull(targetIdx)?.name ?: "",
            appCount = pages.getOrNull(targetIdx)?.apps?.size ?: 0,
            onConfirm = {
                if (pages.size > 1) {
                    val removed = pages.removeAt(targetIdx)
                    pages.firstOrNull()?.apps?.addAll(removed.apps)
                    val safePage = (targetIdx - 1).coerceAtLeast(0)
                    coroutineScope.launch { pagerState.animateScrollToPage(safePage) }
                    saveCurrent()
                }
                deletePageTarget = null
            },
            onDismiss = { deletePageTarget = null }
        )
    }

    if (showSearchNotFound) {
        SearchNotFoundDialog(
            query = searchLastQuery,
            onDismiss = { showSearchNotFound = false }
        )
    }

    if (showSearchResults) {
        SearchResultsDialog(
            query = searchLastQuery,
            results = searchResults,
            onSelect = { result ->
                showSearchResults = false
                coroutineScope.launch { pagerState.animateScrollToPage(result.pageIndex) }
                searchScrollTarget = Pair(result.pageIndex, result.appIndex)
            },
            onDismiss = { showSearchResults = false }
        )
    }

    if (showColorPicker) {
        val currentPage = pages.getOrNull(pagerState.currentPage)
        ColorPickerDialog(
            pageName = currentPage?.name ?: "",
            currentColor = currentPage?.color ?: 0L,
            onColorSelected = { colorLong ->
                currentPage?.color = colorLong
                saveCurrent()
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // ── 画面全体（ドラッグ検出を最上位 Box に置く）─────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A.toInt()))
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
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
                                        val ti = pageApps.indexOfFirst { it.uniqueKey == hoverPkg }
                                        if (fi >= 0 && ti >= 0) pageApps.add(ti, pageApps.removeAt(fi))
                                    }
                            }
                        }
                    },
                    onDragEnd = {
                        val curPkg = draggingPkg
                        val fromPage = draggingFromPage
                        val toPage = dragTargetPage ?: pagerState.currentPage
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
            Spacer(modifier = Modifier.height(statusBarPad + 184.dp))

            if (pages.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = draggingPkg == null,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
                    val gridState = rememberLazyGridState()
                    val isCurrentPage = pagerState.currentPage == pageIndex
                    LaunchedEffect(isCurrentPage) {
                        if (isCurrentPage) gridState.scrollToItem(0)
                    }
                    LaunchedEffect(isCurrentPage, searchScrollTarget) {
                        val target = searchScrollTarget
                        if (isCurrentPage && target != null && target.first == pageIndex) {
                            gridState.animateScrollToItem(target.second)
                            searchScrollTarget = null
                        }
                    }
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
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 10.dp, end = 10.dp, top = 13.dp, bottom = 8.dp
                            )
                        ) {
                            itemsIndexed(
                                items = page.apps,
                                key = { _, app -> app.uniqueKey }
                            ) { _, app ->
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

        // ── ページ名編集中: 枠外タップでキーボードを閉じるオーバーレイ ──
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
        TopBar(
            statusBarPad = statusBarPad,
            isEditMode = isEditMode,
            isRefreshing = isRefreshing,
            isTabMoveMode = isTabMoveMode,
            editingPageName = editingPageName,
            pageNameInput = pageNameInput,
            pages = pages,
            currentPage = pagerState.currentPage,
            tabListState = tabListState,
            focusRequester = focusRequester,
            onRefreshClick = {
                isRefreshing = true
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                    loadKey++
                }
            },
            onResetClick = { showResetDialog = true },
            onTabMoveModeToggle = { isTabMoveMode = !isTabMoveMode },
            onMoveTabLeft = {
                val idx = pagerState.currentPage
                val tab = pages.removeAt(idx)
                pages.add(idx - 1, tab)
                coroutineScope.launch { pagerState.animateScrollToPage(idx - 1) }
                saveCurrent()
            },
            onMoveTabRight = {
                val idx = pagerState.currentPage
                val tab = pages.removeAt(idx)
                pages.add(idx + 1, tab)
                coroutineScope.launch { pagerState.animateScrollToPage(idx + 1) }
                saveCurrent()
            },
            onEditModeDone = { isEditMode = false },
            onColorPickerClick = { showColorPicker = true },
            onTabClick = { index ->
                if (index == pagerState.currentPage && !editingPageName) {
                    pageNameInput = pages[index].name
                    editingPageName = true
                } else if (index != pagerState.currentPage) {
                    keyboardController?.hide()
                    editingPageName = false
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            },
            onPageNameChange = { pageNameInput = it },
            onPageNameDone = {
                val t = pageNameInput.trim()
                if (t.isNotEmpty()) {
                    pages[pagerState.currentPage].name = t
                    saveCurrent()
                }
                keyboardController?.hide()
                editingPageName = false
            },
            onPageAdd = {
                val newPage = PageData("ページ ${pages.size + 1}")
                pages.add(newPage)
                val newIdx = pages.size - 1
                coroutineScope.launch { pagerState.animateScrollToPage(newIdx) }
                saveCurrent()
            },
            onPageDeleteRequest = {
                val currentIdx = pagerState.currentPage
                val currentApps = pages.getOrNull(currentIdx)?.apps
                if (currentApps.isNullOrEmpty()) {
                    pages.removeAt(currentIdx)
                    val safePage = (currentIdx - 1).coerceAtLeast(0)
                    coroutineScope.launch { pagerState.animateScrollToPage(safePage) }
                    saveCurrent()
                } else {
                    deletePageTarget = currentIdx
                }
            },
            onTabDeleteRequest = { index -> deletePageTarget = index },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchSubmit = { onSearchSubmit() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ── ドラッグゴースト ─────────────────────────────────────────────
        if (draggingPkg != null) {
            val draggedApp = pages.flatMap { it.apps }.firstOrNull { it.uniqueKey == draggingPkg }
            if (draggedApp != null) {
                DragGhost(draggedApp = draggedApp, dragPosition = dragPosition)
            }
        }
    }
}
