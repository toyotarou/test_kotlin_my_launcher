package com.example.test_kotlin_my_launcher.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import com.example.test_kotlin_my_launcher.data.model.AppInfo
import kotlin.math.roundToInt

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
