package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ZhiQiTokens.BackgroundTop, ZhiQiTokens.BackgroundBottom)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.9f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZhiQiTokens.AccentSoft.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        radius = 1080f
                    )
                )
        )
        content()
    }
}

fun Modifier.glassCard(): Modifier {
    val shape = RoundedCornerShape(22.dp)
    return this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            ambientColor = ZhiQiTokens.Primary.copy(alpha = 0.14f),
            spotColor = ZhiQiTokens.PrimaryStrong.copy(alpha = 0.12f)
        )
        .clip(shape)
        .background(ZhiQiTokens.Surface.copy(alpha = 0.96f))
        .border(1.dp, ZhiQiTokens.Border, shape)
}

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun SectionTitle(text: String) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
