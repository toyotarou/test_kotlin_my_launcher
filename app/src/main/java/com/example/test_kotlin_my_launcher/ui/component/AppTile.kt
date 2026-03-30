package com.example.test_kotlin_my_launcher.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import com.example.test_kotlin_my_launcher.data.model.AppInfo

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
