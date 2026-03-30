package com.example.test_kotlin_my_launcher.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DeletePageDialog(
    pageName: String,
    appCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ページを削除") },
        text = {
            if (appCount > 0) {
                Text("アプリアイコンが存在しますがこのタブを消して良いですか？\n（${appCount}個のアプリは最初のページへ移動します）")
            } else {
                Text("「$pageName」を削除しますか？")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("いいえ") }
        }
    )
}
