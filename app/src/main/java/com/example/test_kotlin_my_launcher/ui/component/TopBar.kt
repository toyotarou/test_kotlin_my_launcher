package com.example.test_kotlin_my_launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import com.example.test_kotlin_my_launcher.data.model.PageData

@Composable
fun TopBar(
    statusBarPad: Dp,
    isEditMode: Boolean,
    isRefreshing: Boolean,
    isTabMoveMode: Boolean,
    editingPageName: Boolean,
    pageNameInput: String,
    pages: List<PageData>,
    currentPage: Int,
    tabListState: LazyListState,
    focusRequester: FocusRequester,
    onRefreshClick: () -> Unit,
    onResetClick: () -> Unit,
    onTabMoveModeToggle: () -> Unit,
    onMoveTabLeft: () -> Unit,
    onMoveTabRight: () -> Unit,
    onEditModeDone: () -> Unit,
    onColorPickerClick: () -> Unit,
    onTabClick: (index: Int) -> Unit,
    onPageNameChange: (String) -> Unit,
    onPageNameDone: () -> Unit,
    onPageAdd: () -> Unit,
    onPageDeleteRequest: () -> Unit,
    onTabDeleteRequest: (index: Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarPad)
            .background(Color.Black.copy(alpha = if (isEditMode) 0.70f else 0.35f))
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
                        text = "- Amazing Launcher -",
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
                        .clickable(enabled = !isRefreshing) { onRefreshClick() },
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

            // ── アクション行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // リセット
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onResetClick() }
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
                        .clickable { onTabMoveModeToggle() }
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
                val canMoveLeft = isTabMoveMode && currentPage > 0
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
                        .clickable(enabled = canMoveLeft) { onMoveTabLeft() }
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
                val canMoveRight = isTabMoveMode && currentPage < pages.size - 1
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
                        .clickable(enabled = canMoveRight) { onMoveTabRight() }
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

                // 完了ボタン（編集モード時のみ）
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .clickable { onEditModeDone() }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("完了", color = Color(0xFF1A1A2E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // カラーパレットボタン
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
                        .clickable { onColorPickerClick() }
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // ── 検索行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "アプリを検索...",
                                    color = Color.White.copy(alpha = 0.40f),
                                    fontSize = 13.sp
                                )
                            }
                            inner()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onSearchSubmit() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("検索", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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
                        val isActive = currentPage == index
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
                                .clickable { onTabClick(index) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            if (isActive && editingPageName) {
                                BasicTextField(
                                    value = pageNameInput,
                                    onValueChange = onPageNameChange,
                                    modifier = Modifier.focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onPageNameDone() }),
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
                                        .clickable { onTabDeleteRequest(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", color = Color.White, fontSize = 8.sp, lineHeight = 8.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // ＋ ページ追加
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { onPageAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // − ページ削除
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
                        .clickable(enabled = canDelete) { onPageDeleteRequest() },
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
}
