package com.example.test_kotlin_my_launcher.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.test_kotlin_my_launcher.data.model.AppInfo

data class SearchResult(
    val pageIndex: Int,
    val pageName: String,
    val appIndex: Int,
    val app: AppInfo
)

@Composable
fun SearchResultsDialog(
    query: String,
    results: List<SearchResult>,
    onSelect: (SearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(vertical = 20.dp)
        ) {
            // タイトル
            Text(
                text = "「$query」の検索結果",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = "${results.size}件見つかりました",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(result) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = result.app.icon.asImageBitmap(),
                            contentDescription = result.app.label,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = result.app.label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = result.pageName,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
