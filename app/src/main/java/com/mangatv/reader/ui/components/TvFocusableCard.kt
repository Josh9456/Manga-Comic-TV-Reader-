package com.mangatv.reader.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import com.mangatv.reader.ui.theme.AccentCyan
import com.mangatv.reader.ui.theme.CinemaCardBg
import com.mangatv.reader.ui.theme.FocusGlowCyan

@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleFactor: Float = 1.05f,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scaleFactor else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tv_card_scale"
    )

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .scale(animatedScale)
            .shadow(
                elevation = if (isFocused) 16.dp else 2.dp,
                shape = shape,
                spotColor = if (isFocused) FocusGlowCyan else Color.Black
            ),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = CinemaCardBg,
            focusedContainerColor = CinemaCardBg
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, AccentCyan),
                shape = shape
            ),
            border = Border(
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                shape = shape
            )
        ),
        scale = CardScale.None, // We handle smooth animated scaling explicitly
        interactionSource = interactionSource
    ) {
        Box {
            content(isFocused)
        }
    }
}
