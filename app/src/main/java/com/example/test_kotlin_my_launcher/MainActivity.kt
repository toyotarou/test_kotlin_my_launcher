package com.example.test_kotlin_my_launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.core.content.edit

// ────────────────────────────────────────────────────────────────────────
// SharedPreferences ヘルパー
//   Flutter の SharedPreferences に相当。
//   アプリの並び順をカンマ区切り文字列で保存する。
//   削除したアプリは保存リストに含まれないので次回起動時も非表示になる。
// ────────────────────────────────────────────────────────────────────────
private const val PREFS_NAME = "launcher_prefs"
private const val KEY_APP_ORDER = "app_order"

/** 保存済み並び順を読み込む（packageName のリスト） */
fun loadSavedOrder(context: Context): List<String> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_APP_ORDER, null)
    return if (raw.isNullOrEmpty()) emptyList()
    else raw.split(",").filter { it.isNotBlank() }
}

/** 現在の並び順を保存する */
fun saveAppOrder(context: Context, packages: List<String>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit {
            putString(KEY_APP_ORDER, packages.joinToString(","))
        }   // apply() は非同期書き込み（UI スレッドをブロックしない）
}

/** 保存データをすべて消去する（リセット用） */
fun clearAppOrder(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit {
            remove(KEY_APP_ORDER)
        }
}

// ────────────────────────────────────────────────────────────────────────
// Data
// ────────────────────────────────────────────────────────────────────────
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap
)

// ────────────────────────────────────────────────────────────────────────
// Activity
// ────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                LauncherScreen()
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// LauncherScreen
// ────────────────────────────────────────────────────────────────────────
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apps = remember { mutableStateListOf<AppInfo>() }

    // loadKey をインクリメントするたびにアプリリストを再読み込みする
    var loadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(loadKey) {
        apps.clear()
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            // インストール済みアプリをすべて取得（packageName をキーにした Map）
            val allApps = pm
                .queryIntentActivities(intent, 0)
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

            // SharedPreferences から保存済み順序を読む
            val savedOrder = loadSavedOrder(context)

            val display = mutableListOf<AppInfo>()
            if (savedOrder.isEmpty()) {
                // 初回 or リセット後: アルファベット順で全表示
                display.addAll(allApps.values.sortedBy { it.label })
            } else {
                // 保存順に並べる（アンインストール済みアプリは自動スキップ）
                for (pkg in savedOrder) {
                    allApps[pkg]?.let { display.add(it) }
                }
                // 新たにインストールされたアプリは末尾に追加
                val savedSet = savedOrder.toSet()
                allApps.values
                    .filter { it.packageName !in savedSet }
                    .sortedBy { it.label }
                    .forEach { display.add(it) }
            }

            withContext(Dispatchers.Main) {
                apps.addAll(display)
            }
        }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val itemBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var showResetDialog by remember { mutableStateOf(false) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 現在の並び順を SharedPreferences に保存するヘルパー
    fun saveCurrentOrder() {
        val packages = apps.map { it.packageName }
        coroutineScope.launch(Dispatchers.IO) {
            saveAppOrder(context, packages)
        }
    }

    BackHandler(enabled = isEditMode) { isEditMode = false }

    // ── リセット確認ダイアログ ──────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("パネルをリセット") },
            text = {
                Text(
                    "削除したアプリをすべて再表示し、\n" +
                            "並び順も初期状態に戻します。\n" +
                            "よろしいですか？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    isEditMode = false
                    // clear → reload を順番に実行する
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) { clearAppOrder(context) }
                        loadKey++   // LaunchedEffect(loadKey) を再実行
                    }
                }) {
                    Text("リセット", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ── 画面全体 ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            )
    ) {
        // 読み込み中
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("アプリを読み込み中...", color = Color.White, fontSize = 18.sp)
            }
        }

        // アプリグリッド（3 列）
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                // トップバーの高さ分だけ余白を確保
                top = statusBarPadding + 48.dp,
                bottom = navBarPadding + 16.dp
            )
        ) {
            itemsIndexed(
                items = apps,
                key = { _, app -> app.packageName }
            ) { _, app ->
                val isDragging = draggingPkg == app.packageName

                AppTile(
                    app = app,
                    isDragging = isDragging,
                    isEditMode = isEditMode,
                    onDelete = {
                        apps.remove(app)
                        saveCurrentOrder()   // 削除後に保存
                    },
                    onTap = {
                        if (isEditMode) {
                            isEditMode = false   // タイルタップで編集モード終了
                        } else {
                            context.packageManager
                                .getLaunchIntentForPackage(app.packageName)
                                ?.let { context.startActivity(it) }
                        }
                    },
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            itemBoundsMap[app.packageName] = coords.boundsInRoot()
                        }
                        .pointerInput(app.packageName) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { localOffset ->
                                    val bounds = itemBoundsMap[app.packageName]
                                    dragPosition = bounds?.let {
                                        Offset(it.left + localOffset.x, it.top + localOffset.y)
                                    } ?: localOffset
                                    draggingPkg = app.packageName
                                    isEditMode = true
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragPosition += dragAmount
                                    val curPkg = draggingPkg
                                    if (curPkg != null) {
                                        itemBoundsMap.entries
                                            .firstOrNull { (pkg, bounds) ->
                                                pkg != curPkg && bounds.contains(dragPosition)
                                            }
                                            ?.key
                                            ?.let { hoverPkg ->
                                                val fromIdx =
                                                    apps.indexOfFirst { it.packageName == curPkg }
                                                val toIdx =
                                                    apps.indexOfFirst { it.packageName == hoverPkg }
                                                if (fromIdx >= 0 && toIdx >= 0) {
                                                    apps.add(toIdx, apps.removeAt(fromIdx))
                                                }
                                            }
                                    }
                                },
                                onDragEnd = {
                                    draggingPkg = null
                                    dragPosition = Offset.Zero
                                    saveCurrentOrder()   // 並び替え後に保存
                                },
                                onDragCancel = {
                                    draggingPkg = null
                                    dragPosition = Offset.Zero
                                }
                            )
                        }
                )
            }
        }

        // ── トップバー（ステータスバーのすぐ下に固定） ────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .background(Color.Black.copy(alpha = if (isEditMode) 0.70f else 0.30f))
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左側: 編集モード時はヒント表示
                Text(
                    text = if (isEditMode) "✕ タップで削除 / タイルタップで終了" else "MyLauncher",
                    color = Color.White.copy(alpha = if (isEditMode) 0.75f else 0.50f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                // 右側: ボタン群
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // リセットボタン（常に表示）
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFD32F2F).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { }
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

                    // 完了ボタン（編集モード時のみ表示）
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

        // ドラッグ中ゴースト（指に追従）
        if (draggingPkg != null) {
            val draggedApp = apps.firstOrNull { it.packageName == draggingPkg }
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

// ────────────────────────────────────────────────────────────────────────
// AppTile
// ────────────────────────────────────────────────────────────────────────
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
        // パネル本体
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
                    color = if (isDragging) Color.White.copy(alpha = 0.08f) else Color.White.copy(
                        alpha = 0.30f
                    ),
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

        // 削除Xバッジ（編集モード中・非ドラッグ時のみ表示）
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
