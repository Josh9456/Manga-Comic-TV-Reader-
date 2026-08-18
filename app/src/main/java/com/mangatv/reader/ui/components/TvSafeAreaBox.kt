package com.mangatv.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mangatv.reader.ui.theme.CinemaDarkBg

@Composable
fun TvSafeAreaBox(
    modifier: Modifier = Modifier,
    overscanHorizontalPercent: Float = 0.03f, // 3% safe margin default
    overscanVerticalPercent: Float = 0.03f,
    content: @Composable BoxScope.() -> Unit
) {
    // 1080p standard: 3% = ~58px horizontal, ~32px vertical
    val padH: Dp = (1920 * overscanHorizontalPercent / 2).dp
    val padV: Dp = (1080 * overscanVerticalPercent / 2).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaDarkBg)
            .padding(horizontal = padH, vertical = padV)
    ) {
        content()
    }
}
