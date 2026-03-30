package com.example.test_kotlin_my_launcher.ui.theme

import androidx.compose.ui.graphics.Color

const val DEFAULT_PAGE_COLOR = 0xFF3D5A8AL

/** テーマカラーパレット（48色） */
val themeColorPalette: List<Long> = listOf(
    // 深色系
    0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFF8E24AAL, 0xFFFFA726L, 0xFF00ACC1L,
    0xFFFDD835L, 0xFF6D4C41L, 0xFFD81B60L, 0xFF3949ABL, 0xFF00897BL, 0xFF7CB342L,
    0xFF5E35B1L, 0xFFFB8C00L, 0xFF00838FL, 0xFFF4511EL, 0xFF558B2FL, 0xFF6A1B9AL,
    0xFF2E7D32L, 0xFF283593L, 0xFFAD1457L, 0xFF4E342EL, 0xFF1565C0L, 0xFF9E9D24L,
    // 中明度系
    0xFF42A5F5L, 0xFF66BB6AL, 0xFFAB47BCL, 0xFFFFB74DL, 0xFF26C6DAL, 0xFFFFF176L,
    0xFF8D6E63L, 0xFFF06292L, 0xFF5C6BC0L, 0xFF26A69AL, 0xFF9CCC65L, 0xFF9575CDL,
    // 淡色系
    0xFFFFCC80L, 0xFF80DEEAL, 0xFFFFAB91L, 0xFFC5E1A5L, 0xFFB39DDBL, 0xFFA5D6A7L,
    0xFF9FA8DAL, 0xFFF48FB1L, 0xFFBCAAA4L, 0xFFEF5350L, 0xFFBDBDBDL, 0xFFE0E0E0L
)

/** 選択カラーから暗めのグラデーション3色を生成 */
fun themeGradient(base: Color): List<Color> {
    fun dk(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f)
    return listOf(dk(base, 0.30f), dk(base, 0.52f), dk(base, 0.78f))
}
