package com.mangatv.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.AccentOrange
import com.mangatv.reader.ui.theme.AccentTeal
import com.mangatv.reader.ui.theme.BadgeBackground
import com.mangatv.reader.ui.theme.TextDark

enum class BadgeType {
    NEW,
    IN_PROGRESS,
    COMPLETED
}

@Composable
fun TvReadingBadge(
    currentPage: Int,
    totalPages: Int,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalPages > 0) {
        ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    val (text, bgColor, textColor) = when {
        isCompleted -> Triple("100% DONE", AccentTeal, TextDark)
        currentPage > 0 && totalPages > 0 -> Triple("$percentage% • p. $currentPage/$totalPages", AccentOrange, TextDark)
        currentPage > 0 -> Triple("p. $currentPage", AccentOrange, TextDark)
        else -> Triple("NEW", AccentCyan, TextDark)
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

@Composable
fun TvProgressBar(
    currentPage: Int,
    totalPages: Int,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 0) return

    val progressFraction = if (isCompleted) 1f else (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
    val barColor = if (isCompleted) AccentTeal else if (currentPage > 0) AccentOrange else AccentCyan

    Box(
        modifier = modifier
            .background(Color(0x99000000), RoundedCornerShape(2.dp))
    ) {
        if (progressFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressFraction)
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}
