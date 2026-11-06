package com.example.mikrotik.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.mikrotik.ui.theme.AppPalette

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppPalette.Background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMintWaves()
        }
        content()
    }
}

@Composable
fun AppWaveDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawMintWaves()
    }
}

private fun DrawScope.drawMintWaves() {
    val width = size.width
    val height = size.height

    val backWave = Path().apply {
        moveTo(0f, height * 0.87f)
        cubicTo(width * 0.17f, height * 0.78f, width * 0.30f, height * 0.91f, width * 0.51f, height * 0.91f)
        cubicTo(width * 0.73f, height * 0.91f, width * 0.83f, height * 0.80f, width, height * 0.86f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(backWave, color = AppPalette.Mint)

    val frontWave = Path().apply {
        moveTo(0f, height * 0.94f)
        cubicTo(width * 0.20f, height * 0.86f, width * 0.31f, height * 0.98f, width * 0.53f, height * 0.97f)
        cubicTo(width * 0.75f, height * 0.96f, width * 0.86f, height * 0.88f, width, height * 0.91f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(frontWave, color = AppPalette.MintStrong.copy(alpha = 0.72f))
}
