# あめらん 開発記録

> **Amaging Launcher** — Android 向けホームアプリ
> Windows の「cLaunch」を参考に、タブでアプリをカテゴリ分けして管理するランチャーアプリ。

---

## 目次

1. [アプリの概念](#1-アプリの概念)
2. [技術スタック](#2-技術スタック)
3. [ゼロから作る場合の手順](#3-ゼロから作る場合の手順)
4. [ファイル構成](#4-ファイル構成)
5. [引き継いだ時点の実装状態](#5-引き継いだ時点の実装状態)
6. [バグ修正](#6-バグ修正)
7. [追加・改修した機能](#7-追加改修した機能)
8. [データ設計](#8-データ設計)
9. [SharedPreferences のキー一覧](#9-sharedpreferences-のキー一覧)
10. [UI 構造](#10-ui-構造)
11. [アイコン・スプラッシュ画面の更新手順](#11-アイコンスプラッシュ画面の更新手順)

---

## 1. アプリの概念

参考にしたのは Windows アプリ「cLaunch」（https://forest.watch.impress.co.jp/library/software/claunch/）。

**cLaunch の思想**
- タブ（カテゴリ）にアプリを登録して整理する
- ホットキーや左右スワイプでタブを切り替える
- 使いたいアプリにすぐアクセスできる

**あめらん の設計方針**
- このアプリ 1 つをホーム画面に設定し、全アプリをここから起動する
- ページ（タブ）でアプリをカテゴリ分け
- 初回起動時はインストール済みの全アプリを 1 ページ目に表示し、
  ユーザーが削除・移動・並び替えで整理していく

---

## 2. 技術スタック

| 項目 | 内容 |
|---|---|
| 言語 | Kotlin |
| UI フレームワーク | Jetpack Compose（Material 3） |
| 最小 SDK | API 24（Android 7.0） |
| ターゲット / コンパイル SDK | API 36 |
| JVM ターゲット | Java 17 |
| Compose BOM | 2024.10.00 |
| スプラッシュ | androidx.core:core-splashscreen:1.0.1 |

---

## 3. ゼロから作る場合の手順

### 3-1. Android Studio でプロジェクト作成

1. Android Studio を開き **New Project**
2. テンプレート: **Empty Activity**（Jetpack Compose テンプレート）
3. 設定:
   - Name: `test_kotlin_my_launcher`（または任意）
   - Package name: `com.example.test_kotlin_my_launcher`
   - Language: **Kotlin**
   - Minimum SDK: **API 24**
4. Finish

### 3-2. build.gradle.kts（app）の設定

```kotlin
android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.test_kotlin_my_launcher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
```

### 3-3. AndroidManifest.xml の設定

ホームアプリとして動作させるために 2 つの intent-filter が必要。

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Android 11+ のパッケージ可視性対策 -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:allowBackup="true"
        android:supportsRtl="true"
        android:label="@string/app_name">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.MyLauncher.Splash">

            <!-- アプリ一覧から起動するための入口 -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- ホームアプリ候補として登録するための入口 -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

        </activity>
    </application>
</manifest>
```

> **ポイント**: `CATEGORY_HOME` + `CATEGORY_DEFAULT` の両方が必要。
> これがないとホームアプリ候補として表示されない。

### 3-4. スプラッシュテーマの設定

`res/values/themes.xml` にスプラッシュ用テーマを追加：

```xml
<style name="Theme.MyLauncher.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_bg</item>
    <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.MyLauncher</item>
</style>
```

`MainActivity.kt` の `onCreate` で `installSplashScreen()` を **super より前** に呼ぶ：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()   // ← super より必ず前
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { MaterialTheme { LauncherScreen() } }
}
```

### 3-5. ホームアプリとして設定する方法（実機）

1. アプリをビルドしてインストール
2. ホームボタンを押す
3. 「ホームアプリを選択」ダイアログが出たら「あめらん」を選択
4. 「常にこのアプリを使用」を選ぶと次回から自動起動

---

## 4. ファイル構成

```
test_kotlin_my_launcher/
├── operation.md                        ← この文書
├── app/
│   ├── build.gradle.kts                ← 依存関係・SDK 設定
│   └── src/main/
│       ├── AndroidManifest.xml         ← ホームアプリ登録
│       ├── java/com/example/test_kotlin_my_launcher/
│       │   └── MainActivity.kt         ← アプリ全コード（1 ファイル構成）
│       └── res/
│           ├── values/
│           │   ├── strings.xml         ← app_name = "あめらん"
│           │   ├── colors.xml
│           │   └── themes.xml          ← スプラッシュテーマ含む
│           ├── values-night/
│           │   └── themes.xml          ← ダークモード用テーマ
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   └── ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml         ← アダプティブアイコン定義
│           │   └── ic_launcher_round.xml
│           └── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│               ├── ic_launcher.png          ← ランチャーアイコン
│               ├── ic_launcher_foreground.png
│               ├── ic_launcher_background.png
│               ├── ic_launcher_fg.png
│               └── ic_launcher_monochrome.png
```

> **ポイント**: Kotlin + Compose の場合、コードは `MainActivity.kt` 1 ファイルに
> すべて書く構成でも問題なく動く。大規模になれば分割を検討する。

---

## 5. 引き継いだ時点の実装状態

Claude に渡した時点で以下が実装済みだった。

### 実装済み機能

| 機能 | 概要 |
|---|---|
| アプリ一覧取得 | PackageManager で LAUNCHER インテントのアプリを取得、distinctBy で重複除去 |
| 非同期読み込み | `LaunchedEffect` + `Dispatchers.IO` でバックグラウンド処理 |
| 3 列グリッド | `LazyVerticalGrid(GridCells.Fixed(3))` |
| HorizontalPager | 複数ページ対応（左右スワイプ） |
| アプリ起動 | タイルタップで `getLaunchIntentForPackage` |
| ロングプレス→編集モード | アイコンに赤 × バッジ表示 |
| 削除 | × タップでページからアプリを除外 |
| ドラッグ並び替え | `detectDragGesturesAfterLongPress` + `boundsInRoot` |
| ページ間移動 | ドラッグ中に画面端 80px でページ切り替え、ドロップで移動 |
| 永続化 | SharedPreferences にページ名・アプリ順を保存 |
| リセット | 確認ダイアログ → `clearAllSavedData` → 再読み込み |
| ページ追加 | 「+」ボタンで新規ページ |
| ページ削除 | 編集モード時にタブの × から削除 |
| ページ名変更 | タブをタップ → BasicTextField でインライン編集 |
| スプラッシュ | `installSplashScreen()` |
| グラデーション背景 | ページ色から生成した縦グラデーション |

---

## 6. バグ修正

### Bug 1: `draggingFromPage` が未設定（最重要）

**症状**: ドラッグ並び替えとページ間アプリ移動が一切機能しない。

**原因**: `onDragStart` 内で `draggingPkg` はセットしているが、
`draggingFromPage` のセットを忘れていた。
条件式 `if (pagerState.currentPage == draggingFromPage)` が常に false になり、
並び替えロジックが実行されない。

**修正**:
```kotlin
// onDragStart 内に追加
draggingFromPage = fromPage

// onDragEnd / onDragCancel にも追加
draggingFromPage = null
```

### Bug 2: リセットボタンが機能しない

**原因**: `clickable { }` が空のラムダだった。

**修正**: `clickable { showResetDialog = true }`

### Bug 3: リセットダイアログが閉じない

**原因**:
- `onDismissRequest = { }` → 外タップで閉じない
- キャンセルの `onClick = { }` → ボタンを押しても閉じない
- 確定後も `showResetDialog = false` がなかった

**修正**: それぞれに `showResetDialog = false` を追加。

### Bug 4: ページ削除 × ボタンが機能しない

**原因**: タブの × ボタンの `onClick = { }` が空。
`deletePageTarget` が設定されないためダイアログが開かない。

**修正**: `clickable { deletePageTarget = index }`

### Bug 5: ページ削除ダイアログが閉じない

**原因**: キャンセル・確定後に `deletePageTarget = null` がなかった。

**修正**: 両方のボタンに `deletePageTarget = null` を追加。

### Bug 6: ページ名編集でキーボードが消えない

**症状**: `BasicTextField` でページ名を編集中、枠外をタップしてもキーボードが残る。

**修正**:
1. `LocalSoftwareKeyboardController` を取得
2. 全画面を覆う透明オーバーレイを `editingPageName == true` のとき表示
3. オーバーレイタップで `keyboardController?.hide()` + `editingPageName = false`
4. `BackHandler` でも `keyboardController?.hide()` を呼ぶよう修正

```kotlin
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
```

### Bug 7: ページ切り替えが早すぎて途中ページにアイコンを置けない

**原因**: `lastPageScrollMs` の値がエッジスクロール発動後も更新されていなかった。
700ms のクールダウンが機能せず、毎ドラッグイベントでページが切り替わっていた。

**修正**: ページ切り替えを発動した直後に `lastPageScrollMs = now` を代入。
あわせてクールダウンを 700ms → 1200ms に延長。

```kotlin
if (now - lastPageScrollMs > 1200L) {
    val cp = pagerState.currentPage
    when {
        dragPosition.x < 80f && cp > 0 -> {
            lastPageScrollMs = now   // ← これが抜けていた
            dragTargetPage = cp - 1
            coroutineScope.launch { pagerState.animateScrollToPage(cp - 1) }
        }
        dragPosition.x > screenWidthPx - 80f && cp < pages.size - 1 -> {
            lastPageScrollMs = now   // ← これが抜けていた
            dragTargetPage = cp + 1
            coroutineScope.launch { pagerState.animateScrollToPage(cp + 1) }
        }
    }
}
```

### Bug 8: 一部のアイコンが別ページに移動できない

**原因（2 件の複合）**:

**原因 A: アニメーション未完了で指を離すと移動が起きない**

`onDragEnd` で `pagerState.currentPage` を目標ページとして使っていたが、
`animateScrollToPage` のアニメーション完了前に指を離すと
`currentPage` がまだ元のページを指したままになる。
その結果 `fromPage == toPage` と判定されて移動が起きない。

**修正**: `dragTargetPage` 変数を追加してエッジスクロール発動時に目標ページを記録。
`onDragEnd` では `dragTargetPage ?: pagerState.currentPage` を使う。

```kotlin
// onDragStart
dragTargetPage = null

// エッジスクロール発動時
dragTargetPage = cp + 1   // または cp - 1

// onDragEnd
val toPage = dragTargetPage ?: pagerState.currentPage
```

**原因 B: 隣接ページの stale bounds を誤検出する**

HorizontalPager は隣接ページを事前描画するため、`itemBoundsMap` には
全ページのタイル座標が混在している。
初期描画やページ遷移アニメーション中に、隣接ページのタイルが一時的に
画面内の座標で記録されることがある。

`onDragStart` の `itemBoundsMap.entries.firstOrNull` が隣接ページの
タイルにマッチすると `draggingFromPage` が間違ったページになり、
エッジスクロール後 `fromPage == toPage` となって移動が起きない。

**修正**: `onDragStart` で現在表示中のページの packageName セットを作り、
そのセットに含まれるタイルのみをヒット判定の対象にする。

```kotlin
onDragStart = { offset ->
    val currentPageIdx = pagerState.currentPage
    val currentPagePkgs = pages.getOrNull(currentPageIdx)
        ?.apps?.map { it.packageName }?.toSet() ?: emptySet()
    val hit = itemBoundsMap.entries.firstOrNull { (pkg, bounds) ->
        pkg in currentPagePkgs && bounds.contains(offset)   // 現ページのみ対象
    } ?: return@detectDragGesturesAfterLongPress
    draggingPkg = hit.key
    draggingFromPage = currentPageIdx   // 現ページを直接代入（二重チェック不要）
    dragTargetPage = null
    ...
}
```

---

## 7. 追加・改修した機能

### 7-1. タブ UI（ページインジケーターをタブに変更）

**変更前**: 下部にドットインジケーター + 「+」ボタン
**変更後**: 上部にタブ行（ページ名を表示）

**実装ポイント**:
- `LazyRow` + `rememberLazyListState()` でスクロール可能なタブ
- `LaunchedEffect(pagerState.currentPage)` でページ切り替え時にタブを自動スクロール
- アクティブタブ: ページ色 85% 不透明 + 白枠
- 非アクティブタブ: ページ色 35% 不透明 + 薄白枠
- アクティブタブをタップ → ページ名編集（`BasicTextField` にその場で切り替え）
- 非アクティブタブをタップ → ページ切り替え（編集中なら破棄して切り替え）

### 7-2. ページ名編集をタブ内で行う

タブ自体が編集フィールドに変化するインライン編集。

```kotlin
if (isActive && editingPageName) {
    BasicTextField(...)   // タブが入力欄に変化
} else {
    Text(page.name)       // 通常表示
}
```

### 7-3. タブの「+」「−」ボタン（スクロール外に固定）

```
[ タブA  タブB  タブC ... (スクロール) ] [+] [−]
```

- `LazyRow` を `Row` でラップし、`LazyRow` に `Modifier.weight(1f)` を付与
- 「+」「−」は `LazyRow` の外に固定配置（スクロールに追従しない）
- 「−」ボタン:
  - ページが 1 つのみ → 無効（グレーアウト）
  - 現在ページにアプリなし → 確認なしで即削除
  - 現在ページにアプリあり → 確認ダイアログ表示

**削除ダイアログのテキスト分岐**:
```kotlin
if (appCount > 0) {
    Text("アプリアイコンが存在しますがこのタブを消して良いですか？\n（${appCount}個のアプリは最初のページへ移動します）")
} else {
    Text("「$name」を削除しますか？")
}
```

### 7-4. ページごとの背景色

各ページが独立した背景色を持つ。

**データモデルの変更**:
```kotlin
class PageData(name: String, color: Long = DEFAULT_PAGE_COLOR) {
    var name: String by mutableStateOf(name)
    var color: Long by mutableStateOf(color)   // ← 追加
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()
}
```

**背景グラデーション生成**:
```kotlin
fun themeGradient(base: Color): List<Color> {
    fun dk(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f)
    return listOf(dk(base, 0.30f), dk(base, 0.52f), dk(base, 0.78f))
}
```
→ 選択色を 3 段階に暗くした縦グラデーション

**背景はページごとに適用**:
```kotlin
Brush.verticalGradient(
    colors = themeGradient(Color(page.color.toInt()).copy(alpha = 1f))
)
```

**タブもページ色で塗る**:
```kotlin
val pageColor = Color(page.color.toInt())
.background(
    if (isActive) pageColor.copy(alpha = 0.85f)
    else pageColor.copy(alpha = 0.35f),
    RoundedCornerShape(8.dp)
)
```

### 7-5. カラーパレット

アクション行右端に虹色サークルアイコンを配置。タップでダイアログ表示。

**48 色のパレット（6 列 × 8 行）**:

| グループ | 色数 |
|---|---|
| 深色系（ビビッド） | 24色 |
| 中明度系 | 12色 |
| 淡色系（パステル） | 12色 |

```kotlin
val themeColorPalette: List<Long> = listOf(
    // 深色系（24色）
    0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFF8E24AAL, 0xFFFFA726L, 0xFF00ACC1L,
    0xFFFDD835L, 0xFF6D4C41L, 0xFFD81B60L, 0xFF3949ABL, 0xFF00897BL, 0xFF7CB342L,
    0xFF5E35B1L, 0xFFFB8C00L, 0xFF00838FL, 0xFFF4511EL, 0xFF558B2FL, 0xFF6A1B9AL,
    0xFF2E7D32L, 0xFF283593L, 0xFFAD1457L, 0xFF4E342EL, 0xFF1565C0L, 0xFF9E9D24L,
    // 中明度系（12色）
    0xFF42A5F5L, 0xFF66BB6AL, 0xFFAB47BCL, 0xFFFFB74DL, 0xFF26C6DAL, 0xFFFFF176L,
    0xFF8D6E63L, 0xFFF06292L, 0xFF5C6BC0L, 0xFF26A69AL, 0xFF9CCC65L, 0xFF9575CDL,
    // 淡色系（12色）
    0xFFFFCC80L, 0xFF80DEEAL, 0xFFFFAB91L, 0xFFC5E1A5L, 0xFFB39DDBL, 0xFFA5D6A7L,
    0xFF9FA8DAL, 0xFFF48FB1L, 0xFFBCAAA4L, 0xFFEF5350L, 0xFFBDBDBDL, 0xFFE0E0E0L
)
```

**カラーピッカー UI**: `themeColorPalette.chunked(6)` で 6 列に分割して表示。
選択中の色は白枠（3dp）でハイライト。

**保存**: `page_color_N` キーで SharedPreferences に保存。
リセット時は全ページがデフォルト色（ダークブルー `0xFF3D5A8A`）に戻る。

### 7-6. タブ移動ボタン

アクション行に「タブを掴む」「◀」「▶」を追加。

**操作手順**:
1. 移動したいタブを開く
2. 「タブを掴む」をタップ → ボタンが白くなり「タブを離す」に変化（モード維持）
3. 「◀」「▶」で何度でもタブを移動できる
4. 「タブを離す」をタップ → モード解除

> **ポイント**: ◀▶ を押してもモードは解除されない。
> 複数回移動してから最後に「タブを離す」で確定する設計。

**ボタンの有効/無効**:
- `canMoveLeft  = isTabMoveMode && pagerState.currentPage > 0`
- `canMoveRight = isTabMoveMode && pagerState.currentPage < pages.size - 1`

**移動ロジック**（`isTabMoveMode = false` は書かない）:
```kotlin
val idx = pagerState.currentPage
val tab = pages.removeAt(idx)
pages.add(idx - 1, tab)   // 左移動の場合
coroutineScope.launch { pagerState.animateScrollToPage(idx - 1) }
saveCurrent()
// isTabMoveMode は変更しない → モード維持
```

**トグル実装**:
```kotlin
.clickable { isTabMoveMode = !isTabMoveMode }
```
```kotlin
Text(if (isTabMoveMode) "タブを離す" else "タブを掴む")
```

### 7-7. リセットダイアログ

シンプルな確認ダイアログ。

```kotlin
AlertDialog(
    title = { Text("リセット") },
    text  = { Text("リセットして良いですか？") },
    confirmButton = {
        TextButton(onClick = { /* clearAllSavedData → loadKey++ */ }) {
            Text("はい", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        }
    },
    dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("いいえ") }
    }
)
```

### 7-8. アプリタイトル表示

トップバー最上段にアプリ名を表示。

```
あめらん  - Amaging Launcher -
```

- 「あめらん」: 24sp・Bold・文字間隔 2sp（主役）
- 「- Amaging Launcher -」: 11sp・65%白・下端揃え

実装:
```kotlin
Row(verticalAlignment = Alignment.Bottom) {
    Text("あめらん", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.width(10.dp))
    Text("- Amaging Launcher -", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
}
```

### 7-9. アプリ名の変更

`res/values/strings.xml`:
```xml
<string name="app_name">あめらん</string>
```

---

## 8. データ設計

### PageData クラス

```kotlin
class PageData(name: String, color: Long = DEFAULT_PAGE_COLOR) {
    var name: String by mutableStateOf(name)       // ページ名（Compose State）
    var color: Long by mutableStateOf(color)       // 背景色（ARGB Long）
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()  // アプリリスト
}
```

### AppInfo データクラス

```kotlin
data class AppInfo(
    val packageName: String,   // 例: "com.google.android.youtube"
    val label: String,         // 表示名（例: "YouTube"）
    val icon: Bitmap           // 96×96px にリサイズ済み
)
```

### 起動時のデータ読み込みフロー

```
LaunchedEffect(loadKey)
  ↓
IO スレッドで実行
  ├─ PackageManager でインストール済みアプリ取得
  │    └─ distinctBy(packageName) → toBitmap(96, 96)
  └─ SharedPreferences からページデータ読み込み
  ↓
保存データなし → 全アプリをアルファベット順に 1 ページ目へ
保存データあり → 順序復元 + 新規インストールアプリを最終ページ末尾に追加
```

---

## 9. SharedPreferences のキー一覧

| キー | 型 | 内容 |
|---|---|---|
| `page_count` | Int | ページ数 |
| `page_name_N` | String | N 番目のページ名（0 始まり）|
| `page_apps_N` | String | N 番目のページのアプリ順（packageName をカンマ区切り）|
| `page_color_N` | Long | N 番目のページの背景色（ARGB Long）|

**定数定義**（`MainActivity.kt` 上部）:
```kotlin
private const val PREFS_NAME     = "launcher_prefs"
private const val KEY_PAGE_COUNT = "page_count"
private const val KEY_PAGE_NAME  = "page_name_"
private const val KEY_PAGE_APPS  = "page_apps_"
private const val KEY_PAGE_COLOR = "page_color_"
private val DEFAULT_PAGE_COLOR   = 0xFF3D5A8AL
```

---

## 10. UI 構造

```
Box（画面全体・ドラッグジェスチャー担当）
│
├─ Column（コンテンツ）
│   ├─ Spacer（トップバー分の余白: statusBarPad + 140.dp）
│   ├─ HorizontalPager（ページ表示・weight(1f)）
│   │   └─ LazyVerticalGrid（3列・アプリタイル）
│   │       └─ AppTile（アイコン + ラベル + ×バッジ）
│   └─ Spacer（ナビゲーションバー分の余白: navBarPad + 8.dp）
│
├─ Box（枠外タップオーバーレイ・キーボード閉じ用）
│   ※ editingPageName == true のときだけ表示
│
├─ Box（トップバー・Alignment.TopCenter）
│   └─ Column
│       ├─ Row（タイトル行）   ← "あめらん - Amaging Launcher -"
│       ├─ Spacer(5dp)
│       ├─ Row（アクション行） ← リセット / タブを掴む / ◀ / ▶ / 完了 / 🎨
│       ├─ Spacer(5dp)
│       └─ Row（タブ行）
│           ├─ LazyRow（タブ・weight(1f)・スクロール可）
│           │   └─ タブアイテム（ページ名 or BasicTextField + ×バッジ）
│           ├─ [+] ボタン（固定）
│           └─ [−] ボタン（固定）
│
└─ Column（ドラッグゴースト・draggingPkg != null のときだけ表示・zIndex(100f)）
```

### ドラッグ実装の設計思想

ドラッグジェスチャーを **最上位 Box** に置いている理由：
HorizontalPager がページアニメーションを行うと、
その子 View のジェスチャーはキャンセルされてしまう。
最上位 Box でドラッグを検知することで、
ページ切り替えアニメーション中もドラッグが継続する。

```
最上位 Box の detectDragGesturesAfterLongPress
│
├─ onDragStart
│   ├─ 現在ページの packageName セットを作る
│   ├─ そのセット内で boundsInRoot がタップ座標を含むタイルを特定
│   ├─ draggingPkg / draggingFromPage / dragTargetPage を設定
│   └─ isEditMode = true
│
├─ onDrag
│   ├─ 画面左端 80px かつ 1200ms 経過 → 前ページへ切り替え & dragTargetPage 更新
│   ├─ 画面右端 80px かつ 1200ms 経過 → 次ページへ切り替え & dragTargetPage 更新
│   └─ 同ページ内（currentPage == draggingFromPage）の bounds と照合 → 並び替え
│
├─ onDragEnd
│   ├─ toPage = dragTargetPage ?: pagerState.currentPage
│   ├─ fromPage != toPage → ページ間でアプリ移動
│   └─ 各変数をリセット → saveCurrent()
│
└─ onDragCancel → 各変数をリセット
```

**HorizontalPager の設定**:
```kotlin
HorizontalPager(
    state = pagerState,
    userScrollEnabled = draggingPkg == null,  // ドラッグ中はスワイプ無効
)
```

---

## 11. アイコン・スプラッシュ画面の更新手順

### 必要な画像ファイルと配置先

| フォルダ | ランチャーアイコン | フォアグラウンド等 |
|---|---|---|
| mipmap-mdpi | 48×48 px | 108×108 px |
| mipmap-hdpi | 72×72 px | 162×162 px |
| mipmap-xhdpi | 96×96 px | 216×216 px |
| mipmap-xxhdpi | 144×144 px | 324×324 px |
| mipmap-xxxhdpi | 192×192 px | 432×432 px |

**各フォルダに配置するファイル**:
- `ic_launcher.png`（ランチャーサイズ）
- `ic_launcher_fg.png`（ランチャーサイズ）
- `ic_launcher_foreground.png`（フォアグラウンドサイズ）
- `ic_launcher_background.png`（フォアグラウンドサイズ）
- `ic_launcher_monochrome.png`（フォアグラウンドサイズ）

### macOS での一括リサイズスクリプト

```bash
SRC="/path/to/your/ic_launcher.png"
RES="/path/to/project/app/src/main/res"

declare -A LAUNCHER_SIZES=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
declare -A FG_SIZES=(       [mdpi]=108 [hdpi]=162 [xhdpi]=216 [xxhdpi]=324 [xxxhdpi]=432 )

for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  FOLDER="$RES/mipmap-$density"
  LS=${LAUNCHER_SIZES[$density]}
  FS=${FG_SIZES[$density]}

  for f in ic_launcher.png ic_launcher_fg.png; do
    sips -z $LS $LS "$SRC" --out "$FOLDER/$f" > /dev/null
  done

  for f in ic_launcher_foreground.png ic_launcher_background.png ic_launcher_monochrome.png; do
    sips -z $FS $FS "$SRC" --out "$FOLDER/$f" > /dev/null
  done
done
```

> `sips` は macOS 標準コマンドのため追加インストール不要。
> スプラッシュ画面は `ic_launcher_foreground.png` を使用するため、
> このスクリプトで一括更新される。

---

*最終更新: 2026-03-27*
