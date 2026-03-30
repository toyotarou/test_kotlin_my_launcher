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
12. [ファイル分割アーキテクチャ](#12-ファイル分割アーキテクチャ)

### セクション 7 サブ目次
- 7-1 タブ UI
- 7-2 ページ名編集（タブ内インライン）
- 7-3 タブの「+」「−」ボタン
- 7-4 ページごとの背景色
- 7-5 カラーパレット
- 7-6 タブ移動ボタン
- 7-7 リセットダイアログ
- 7-8 アプリタイトル表示 ＋ リロードボタン（isRefreshing 対応）
- 7-9 アプリ名の変更
- 7-10 ピン留めショートカット対応
- 7-11 「ページ1へ戻す」バッジ
- 7-12 画面縦固定
- 7-13 ページ切り替え時の頭出し

> 最終更新: 2026-03-30

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
| マテリアルアイコン | androidx.compose.material:material-icons-core（BOM 管理）|

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
    implementation("androidx.compose.material:material-icons-core")
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
            android:launchMode="singleTask"
            android:screenOrientation="portrait"
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

            <!-- Chrome など「ホーム画面に追加」のショートカットピン留めを受け付ける -->
            <intent-filter>
                <action android:name="android.content.pm.action.CONFIRM_PIN_SHORTCUT" />
            </intent-filter>

        </activity>
    </application>
</manifest>
```

> **ポイント**:
> - `CATEGORY_HOME` + `CATEGORY_DEFAULT` の両方が必要（ホームアプリ候補として表示）。
> - `android:screenOrientation="portrait"` で縦固定（横回転しなくなる）。
> - `android:launchMode="singleTask"` でホームボタンで戻ったとき Activity を再生成しない。
> - `CONFIRM_PIN_SHORTCUT` の intent-filter でピン留めショートカットを受け付ける。

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
├── operation.md                              ← この文書
├── app/
│   ├── build.gradle.kts                      ← 依存関係・SDK 設定
│   └── src/main/
│       ├── AndroidManifest.xml               ← ホームアプリ登録・縦固定
│       ├── java/com/example/test_kotlin_my_launcher/
│       │   │
│       │   ├── MainActivity.kt               ← Activity だけ（OS の窓口）
│       │   │
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── AppInfo.kt            ← AppInfo data class
│       │   │   │   └── PageData.kt           ← PageData, SavedPageData
│       │   │   └── repository/
│       │   │       └── LauncherRepository.kt ← SharedPrefs 読み書き
│       │   │
│       │   └── ui/
│       │       ├── theme/
│       │       │   └── LauncherTheme.kt      ← カラーパレット・グラデーション
│       │       ├── component/
│       │       │   ├── AppTile.kt            ← アプリタイル部品
│       │       │   ├── TopBar.kt             ← ヘッダー全体（タブ行含む）
│       │       │   └── DragGhost.kt          ← ドラッグ中のゴースト
│       │       ├── dialog/
│       │       │   ├── ResetDialog.kt
│       │       │   ├── DeletePageDialog.kt
│       │       │   └── ColorPickerDialog.kt
│       │       └── screen/
│       │           └── LauncherScreen.kt     ← 画面全体の状態管理
│       │
│       └── res/
│           ├── values/
│           │   ├── strings.xml               ← app_name = "あめらん"
│           │   ├── colors.xml
│           │   └── themes.xml                ← スプラッシュテーマ含む
│           ├── values-night/
│           │   └── themes.xml                ← ダークモード用テーマ
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   └── ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml           ← アダプティブアイコン定義
│           │   └── ic_launcher_round.xml
│           └── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│               ├── ic_launcher.png
│               ├── ic_launcher_foreground.png
│               ├── ic_launcher_background.png
│               ├── ic_launcher_fg.png
│               └── ic_launcher_monochrome.png
```

> **ポイント**: レイヤードアーキテクチャで分割している。
> `data/` → `ui/theme, component, dialog` → `ui/screen` → `MainActivity` の順に依存する。
> 詳細は [セクション 12](#12-ファイル分割アーキテクチャ) を参照。

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

### Bug 9: 新規インストールアプリが page 0 ではなく最終ページに表示される

**症状**: アプリをインストールした後、再読み込みすると最終ページの末尾に現れる。

**原因**: 保存データがある場合の新規アプリ追加先が `created.lastOrNull()` になっていた。

**修正**:
```kotlin
// 修正前
.let { created.lastOrNull()?.apps?.addAll(it) }

// 修正後
.let { created.firstOrNull()?.apps?.addAll(it) }
```

### Bug 10: アンインストール済みアプリのアイコンが残り続ける

**症状**: アプリをアンインストールしても、そのアイコンが各ページに残り続ける。

**原因**: SharedPreferences に保存された packageName のリストを復元する際、
PackageManager にもはや存在しない packageName でも `allAppsMap[pkg]` が `null` を返すだけで
特にエラーにならない。表示からは消えるが、アプリを再起動するまで反映されない。

**修正**: タイトル行右端にリロードボタン（`Icons.Default.Refresh`）を追加。
押下で `loadKey++` → `LaunchedEffect(loadKey)` が再実行され、
PackageManager から最新のアプリ一覧を取得し直す。
アンインストール済みアプリは `allAppsMap` に存在しないため自動的に除外される。

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

### 7-8. アプリタイトル表示 ＋ リロードボタン

トップバー最上段にアプリ名とリロードボタンを表示。

```
あめらん  - Amaging Launcher -          [↺]
```

- 「あめらん」: 24sp・Bold・文字間隔 2sp（主役）
- 「- Amaging Launcher -」: 11sp・65%白・下端揃え
- `[↺]`: 右端に 36dp 円形アイコンボタン（`Icons.Default.Refresh`）

**レイアウト**: `Arrangement.SpaceBetween` で左グループ（タイトル）と右アイコンを両端配置。

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, ...),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("あめらん", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.width(10.dp))
        Text("- Amaging Launcher -", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { loadKey++ },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Refresh, contentDescription = "リロード", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
```

**リロード動作**: `loadKey++` → `LaunchedEffect(loadKey)` が再実行され PackageManager から
最新のアプリ一覧を取得。アンインストール済みアプリは自動除外。新規インストールアプリは page 0 末尾に追加。

**ローディング中の表示切り替え**:
- `isRefreshing: Boolean` state を追加
- リロード開始時: `isRefreshing = true` → CircularProgressIndicator を表示
- `LaunchedEffect(loadKey)` 終了時: `isRefreshing = false` → 通常の Refresh アイコンに戻る
- リロード中はボタンを無効化（`clickable(enabled = !isRefreshing)`）
- ページ 0 へ自動スクロール後にリロード: `pagerState.animateScrollToPage(0); loadKey++`

**追加 import**:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
```

**追加 dependency** (`build.gradle.kts`):
```kotlin
implementation("androidx.compose.material:material-icons-core")
```

### 7-9. アプリ名の変更

`res/values/strings.xml`:
```xml
<string name="app_name">あめらん</string>
```

### 7-10. ピン留めショートカット対応（Chrome「ホーム画面に追加」）

Chrome や他のアプリから「ホーム画面に追加」でピン留めされたショートカットをアプリ一覧に表示・起動できる。

**受信フロー**:
1. `CONFIRM_PIN_SHORTCUT` インテントを `MainActivity.onNewIntent` で受け取る
2. `LauncherApps.getPinItemRequest(intent)?.accept()` で承諾
3. `_pinTrigger.intValue++` → `LaunchedEffect(pinTrigger)` → `loadKey++` で再読み込み

```kotlin
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
```

**読み込みフロー**（`LaunchedEffect(loadKey)` 内）:
- `LauncherApps.getShortcuts(query, Process.myUserHandle())` でピン留めショートカットを取得
- API 29+ では `FLAG_MATCH_PINNED_BY_ANY_LAUNCHER` を追加（他のランチャーでピン留めされたものも含む）
- `AppInfo(shortcutId = info.id)` として通常アプリと同列に管理
- `hasShortcutHostPermission()` が false のとき（デフォルトホームアプリでない状態）は空リストを返す

**起動**（`AppTile` の `onTap`）:
```kotlin
if (app.shortcutId != null) {
    la.startShortcut(app.packageName, app.shortcutId, null, null, Process.myUserHandle())
} else {
    packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
}
```

**保存**: `uniqueKey`（`s:pkg/id` 形式）で SharedPreferences に保存。再起動後も復元される。

**必要な API**: `LauncherApps` は API 21+、`getShortcuts()` は API 25+。

### 7-11. 「ページ1へ戻す」バッジ（AppTile 左上）


ページ 2 以降にいるとき、編集モード中のアプリタイル左上に青い「↩」バッジを表示。
タップするとそのアプリをページ 1 の末尾に移動する。

**AppTile の引数**:
```kotlin
onMoveToFirst: (() -> Unit)? = null  // null = ページ1にいる場合は非表示
```

**呼び出し側**（`LauncherScreen`）:
```kotlin
onMoveToFirst = if (pageIndex > 0) {
    {
        page.apps.remove(app)
        pages.firstOrNull()?.apps?.add(app)
        saveCurrent()
    }
} else null
```

**バッジ表示**:
```kotlin
if (onMoveToFirst != null) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .align(Alignment.TopStart)
            .offset(x = (-3).dp, y = (-3).dp)
            .background(Color(0xFF1565C0), CircleShape)
            .border(1.5.dp, Color.White, CircleShape)
            .clickable(onClick = onMoveToFirst),
        contentAlignment = Alignment.Center
    ) {
        Text("↩", color = Color.White, fontSize = 10.sp)
    }
}
```

### 7-12. 画面縦固定

端末を横向きにしてもレイアウトが崩れないよう、縦固定にする。

**設定箇所**: `AndroidManifest.xml` の `<activity>` 要素に 1 行追加するだけ。

```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="portrait"   ← この行を追加
    ...>
```

> **Kotlin コードの変更は不要**。Manifest だけで完結する。

---

### 7-13. ページ切り替え時のグリッド頭出し

別のタブに切り替えたとき、そのページが途中までスクロールされていても
自動的に先頭（一番上）へ戻るようにする。

**実装ポイント**:
- `LazyVerticalGrid` に `rememberLazyGridState()` を渡して状態を持たせる
- ページがアクティブになった（`isCurrentPage == true`）タイミングで `scrollToItem(0)` を呼ぶ

**コード（`LauncherScreen.kt` の HorizontalPager ブロック内）**:

```kotlin
HorizontalPager(state = pagerState, ...) { pageIndex ->
    val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager

    // ① ページごとに独立したスクロール状態を生成
    val gridState = rememberLazyGridState()

    // ② このページが現在表示中かを監視
    val isCurrentPage = pagerState.currentPage == pageIndex

    // ③ アクティブになった瞬間に先頭へスクロール
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) gridState.scrollToItem(0)
    }

    Box(...) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,   // ← ④ 状態を渡す
            ...
        ) { ... }
    }
}
```

**なぜ `rememberLazyGridState()` が必要か**:
- `LazyVerticalGrid` はデフォルトでは内部に状態を持つが、外から `scrollToItem()` を
  呼ぶには `LazyGridState` のインスタンスが必要。
- `HorizontalPager` の各ページは独立した Composable なので、
  それぞれ `remember` で別々の `LazyGridState` が生成される。

**追加した import**:
```kotlin
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
    val icon: Bitmap,          // 96×96px にリサイズ済み
    val shortcutId: String? = null  // null = 通常アプリ, non-null = ピン留めショートカット
) {
    // ドラッグ追跡・グリッドキー・保存キーに使う一意キー
    // 通常アプリ: packageName と同じ
    // ショートカット: "s:packageName/shortcutId" 形式
    val uniqueKey: String get() = if (shortcutId != null) "s:$packageName/$shortcutId" else packageName
}
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
保存データあり → 順序復元 + 新規インストールアプリを先頭ページ（page 0）末尾に追加
```

---

## 9. SharedPreferences のキー一覧

| キー | 型 | 内容 |
|---|---|---|
| `page_count` | Int | ページ数 |
| `page_name_N` | String | N 番目のページ名（0 始まり）|
| `page_apps_N` | String | N 番目のページのアプリ順（`uniqueKey` をカンマ区切り。通常アプリは packageName、ショートカットは `s:pkg/id`）|
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
│       ├─ Row（タイトル行）   ← 左: "あめらん - Amaging Launcher -" / 右: ↺ リロードボタン
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

---

## 12. ファイル分割アーキテクチャ

当初は `MainActivity.kt` 1 ファイルにすべて書いていたが、
コードが肥大化したためレイヤードアーキテクチャに基づいて分割した。

### 12-1. 分割の基本思想

**「役割ごとにファイルを分ける」**

| レイヤー | フォルダ | 何を書くか |
|---|---|---|
| データ定義 | `data/model/` | データの「形」（data class）|
| データ保存 | `data/repository/` | SharedPreferences の読み書き |
| テーマ定数 | `ui/theme/` | 色・スタイルの定数・関数 |
| UI 部品 | `ui/component/` | 再利用できる小さな Composable |
| ダイアログ | `ui/dialog/` | AlertDialog を返す Composable |
| 画面 | `ui/screen/` | 状態管理・レイアウト全体 |
| Activity | ルート直下 | OS との窓口（最小限）|

**依存の方向（下が上を知らない）**:

```
MainActivity
    └── ui/screen/LauncherScreen
            ├── ui/component/AppTile, TopBar, DragGhost
            ├── ui/dialog/ResetDialog, DeletePageDialog, ColorPickerDialog
            ├── ui/theme/LauncherTheme
            ├── data/repository/LauncherRepository
            └── data/model/AppInfo, PageData
```

### 12-2. 各ファイルの役割と package 宣言

Kotlin はファイルの先頭に `package` を書く。
フォルダ構造と package 名は一致させるのがルール。

| ファイル | package |
|---|---|
| `MainActivity.kt` | `com.example.test_kotlin_my_launcher` |
| `data/model/AppInfo.kt` | `com.example.test_kotlin_my_launcher.data.model` |
| `data/model/PageData.kt` | `com.example.test_kotlin_my_launcher.data.model` |
| `data/repository/LauncherRepository.kt` | `com.example.test_kotlin_my_launcher.data.repository` |
| `ui/theme/LauncherTheme.kt` | `com.example.test_kotlin_my_launcher.ui.theme` |
| `ui/component/AppTile.kt` | `com.example.test_kotlin_my_launcher.ui.component` |
| `ui/component/TopBar.kt` | `com.example.test_kotlin_my_launcher.ui.component` |
| `ui/component/DragGhost.kt` | `com.example.test_kotlin_my_launcher.ui.component` |
| `ui/dialog/ResetDialog.kt` | `com.example.test_kotlin_my_launcher.ui.dialog` |
| `ui/dialog/DeletePageDialog.kt` | `com.example.test_kotlin_my_launcher.ui.dialog` |
| `ui/dialog/ColorPickerDialog.kt` | `com.example.test_kotlin_my_launcher.ui.dialog` |
| `ui/screen/LauncherScreen.kt` | `com.example.test_kotlin_my_launcher.ui.screen` |

### 12-3. 別ファイルのものを使う方法（import）

他のファイルに定義されたクラスや関数を使うには `import` 文が必要。

```kotlin
// 例: LauncherScreen.kt から AppTile を使いたいとき
import com.example.test_kotlin_my_launcher.ui.component.AppTile

// パッケージ内の全部を一括 import する場合
import com.example.test_kotlin_my_launcher.data.model.*
```

> Android Studio では `Alt + Enter`（Mac: `Option + Enter`）で
> 未解決の参照を自動 import できる。

### 12-4. 各ファイルの内容

#### `data/model/AppInfo.kt`

```kotlin
package com.example.test_kotlin_my_launcher.data.model

import android.graphics.Bitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val shortcutId: String? = null
) {
    val uniqueKey: String
        get() = if (shortcutId != null) "s:$packageName/$shortcutId" else packageName
}
```

#### `data/model/PageData.kt`

```kotlin
package com.example.test_kotlin_my_launcher.data.model

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.test_kotlin_my_launcher.ui.theme.DEFAULT_PAGE_COLOR

data class SavedPageData(val name: String, val appOrder: List<String>, val color: Long)

class PageData(name: String, color: Long = DEFAULT_PAGE_COLOR) {
    var name: String by mutableStateOf(name)
    var color: Long by mutableStateOf(color)
    val apps: SnapshotStateList<AppInfo> = mutableStateListOf()
}
```

> **注意**: `PageData` は `by mutableStateOf` を使うため `data class` にできない。
> `mutableStateOf` は Compose の状態管理システムに登録するための委譲プロパティ。

#### `data/repository/LauncherRepository.kt`

SharedPreferences の読み書きを 3 つのトップレベル関数としてまとめたファイル。
クラスは不要（Kotlin はクラス外に関数を書ける）。

```kotlin
package com.example.test_kotlin_my_launcher.data.repository

fun loadSavedPages(context: Context): List<SavedPageData> { ... }
fun saveAllPages(context: Context, pages: List<PageData>) { ... }
fun clearAllSavedData(context: Context) { ... }
```

> **Kotlin の特徴**: Java と違い、関数はクラスの中に書かなくてもよい。
> ファイル直下に書いた関数は「トップレベル関数」と呼ぶ。

#### `ui/theme/LauncherTheme.kt`

色の定数とグラデーション生成関数。UI コンポーネントから参照される。

```kotlin
package com.example.test_kotlin_my_launcher.ui.theme

const val DEFAULT_PAGE_COLOR = 0xFF3D5A8AL   // Long リテラル

val themeColorPalette: List<Long> = listOf( ... )

fun themeGradient(base: Color): List<Color> { ... }
```

#### `ui/component/AppTile.kt`

アプリ 1 タイル分の Composable。引数でデータと操作ラムダを受け取る。

```kotlin
@Composable
fun AppTile(
    app: AppInfo,
    isDragging: Boolean,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onTap: () -> Unit,
    onMoveToFirst: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) { ... }
```

> **Composable の設計原則**:
> - データ（`AppInfo`）と操作（`onDelete` など）を引数で受け取る
> - 自分では状態を持たない（ステートレス設計）
> - `modifier` は最後の引数にして外から追加できるようにする

#### `ui/component/TopBar.kt`

ヘッダー全体（タイトル行・アクション行・タブ行）を 1 Composable にまとめたファイル。
多くのコールバックを引数で受け取る設計。

```kotlin
@Composable
fun TopBar(
    // 表示に必要なデータ
    statusBarPad: Dp,
    isEditMode: Boolean,
    pages: List<PageData>,
    currentPage: Int,
    // ...
    // 操作ラムダ（イベントハンドラ）
    onRefreshClick: () -> Unit,
    onResetClick: () -> Unit,
    onTabClick: (index: Int) -> Unit,
    // ...
    modifier: Modifier = Modifier,
) { ... }
```

#### `ui/component/DragGhost.kt`

ドラッグ中に指に追従するゴーストアイコン。

```kotlin
@Composable
fun DragGhost(
    draggedApp: AppInfo,
    dragPosition: Offset
) {
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
        ...
    ) { ... }
}
```

> **`offset { IntOffset(...) }` の書き方**:
> `offset` の引数ラムダは `Density` スコープで実行されるため、
> `ghostSize.toPx()` のように `Dp → px` 変換が書ける。

#### `ui/dialog/` の 3 ファイル

各ダイアログを独立した Composable に切り出したもの。
`AlertDialog` の引数に必要な情報をパラメータとして受け取る。

```kotlin
// ResetDialog.kt
@Composable
fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) { ... }

// DeletePageDialog.kt
@Composable
fun DeletePageDialog(
    pageName: String, appCount: Int,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) { ... }

// ColorPickerDialog.kt
@Composable
fun ColorPickerDialog(
    pageName: String, currentColor: Long,
    onColorSelected: (Long) -> Unit, onDismiss: () -> Unit
) { ... }
```

#### `ui/screen/LauncherScreen.kt`

画面全体の状態（`isEditMode`、`draggingPkg` など）を管理するファイル。
分割後もこのファイルは長い（約 300 行）が、それは状態管理の責務が集まっているため。

- `var isEditMode by remember { mutableStateOf(false) }` などの状態変数宣言
- `LaunchedEffect` によるデータ読み込み
- ドラッグジェスチャーのロジック
- ダイアログ・TopBar・HorizontalPager の呼び出し（実装ではなく組み合わせ）

#### `MainActivity.kt`（分割後）

```kotlin
package com.example.test_kotlin_my_launcher

class MainActivity : ComponentActivity() {
    private val _pinTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptPinShortcut(intent)
        setContent { MaterialTheme { LauncherScreen(pinTrigger = _pinTrigger.intValue) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptPinShortcut(intent)
    }

    private fun acceptPinShortcut(intent: Intent?) { ... }
}
```

Activity が知るのは `LauncherScreen` だけ。他の Composable は知らない。

### 12-5. ファイル作成時のチェックリスト

新しいファイルを追加するときの手順：

1. **フォルダを作成**（Android Studio のプロジェクトビューで右クリック → New → Package）
2. **ファイルを作成**（New → Kotlin Class/File）
3. **先頭に `package` 宣言を書く**（フォルダ構造と一致させる）
4. **使いたい側で `import` する**（`Alt + Enter` で自動補完）
5. **ビルドして確認**（`./gradlew compileDebugKotlin`）

### 12-6. よくある分割の失敗

| 失敗 | 原因 | 対処 |
|---|---|---|
| `Unresolved reference` エラー | import が足りない | `Alt + Enter` で自動 import |
| `package` が間違っている | フォルダ名とpackage名が不一致 | ファイル先頭の `package` をフォルダ構造に合わせる |
| 循環参照 | A が B を import、B が A を import | 依存方向を見直す（data ← ui の方向を守る） |
| `DEFAULT_PAGE_COLOR` が未解決 | `PageData.kt` が `LauncherTheme.kt` を import する前に書いた | import 文を追加する |

---

*最終更新: 2026-03-30（7-12 画面縦固定、7-13 ページ頭出し追加、ファイル構成を多ファイル構成に更新、セクション 12 追加）*
