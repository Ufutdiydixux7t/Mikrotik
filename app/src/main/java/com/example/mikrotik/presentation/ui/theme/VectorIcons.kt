package com.example.mikrotik.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    val AntennaHotspot: ImageVector
        get() = ImageVector.Builder(
            name = "AntennaHotspot",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Central Tower
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                lineTo(8f, 21f)
                moveTo(12f, 9f)
                lineTo(16f, 21f)
                moveTo(10f, 15f)
                lineTo(14f, 15f)
            }
            // Signal Waves
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Inner Left
                moveTo(8.5f, 7.5f)
                curveTo(7.5f, 9f, 7.5f, 11f, 8.5f, 12.5f)
                // Inner Right
                moveTo(15.5f, 7.5f)
                curveTo(16.5f, 9f, 16.5f, 11f, 15.5f, 12.5f)
                // Outer Left
                moveTo(6f, 5f)
                curveTo(4f, 7.5f, 4f, 12.5f, 6f, 15f)
                // Outer Right
                moveTo(18f, 5f)
                curveTo(20f, 7.5f, 20f, 12.5f, 18f, 15f)
            }
        }.build()

    val WifiSignal: ImageVector
        get() = ImageVector.Builder(
            name = "WifiSignal",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Arc 1 (Top)
                moveTo(3f, 7f)
                curveTo(8f, 2.5f, 16f, 2.5f, 21f, 7f)
                // Arc 2 (Middle)
                moveTo(6.5f, 11f)
                curveTo(9.5f, 7.5f, 14.5f, 7.5f, 17.5f, 11f)
                // Arc 3 (Inner)
                moveTo(10f, 15f)
                curveTo(11.2f, 13.5f, 12.8f, 13.5f, 14f, 15f)
            }
            path(fill = SolidColor(Color(0xFF0E6C7D))) {
                moveTo(12f, 18.5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 12.01f, 18.5f)
                close()
            }
        }.build()

    val MobileDataArrows: ImageVector
        get() = ImageVector.Builder(
            name = "MobileDataArrows",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Up Arrow
                moveTo(9f, 19f)
                lineTo(9f, 5f)
                moveTo(9f, 5f)
                lineTo(5f, 9f)
                moveTo(9f, 5f)
                lineTo(13f, 9f)

                // Down Arrow
                moveTo(15f, 5f)
                lineTo(15f, 19f)
                moveTo(15f, 19f)
                lineTo(11f, 15f)
                moveTo(15f, 19f)
                lineTo(19f, 15f)
            }
        }.build()

    val ConnectedDevices: ImageVector
        get() = ImageVector.Builder(
            name = "ConnectedDevices",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0D2B45)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Laptop Monitor
                moveTo(2f, 6f)
                lineTo(16f, 6f)
                lineTo(16f, 16f)
                lineTo(2f, 16f)
                close()
                moveTo(0f, 19f)
                lineTo(18f, 19f)

                // Phone
                moveTo(15f, 10f)
                lineTo(22f, 10f)
                lineTo(22f, 21f)
                lineTo(15f, 21f)
                close()
            }
        }.build()

    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 10f)
                lineTo(12f, 3f)
                lineTo(21f, 10f)
                lineTo(21f, 21f)
                lineTo(15f, 21f)
                lineTo(15f, 14f)
                lineTo(9f, 14f)
                lineTo(9f, 21f)
                lineTo(3f, 21f)
                close()
            }
        }.build()

    val CreditCard: ImageVector
        get() = ImageVector.Builder(
            name = "CreditCard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF757575)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 6f)
                lineTo(22f, 6f)
                lineTo(22f, 18f)
                lineTo(2f, 18f)
                close()
                moveTo(2f, 10f)
                lineTo(22f, 10f)
            }
        }.build()

    val BarChart: ImageVector
        get() = ImageVector.Builder(
            name = "BarChart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF757575)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 20f)
                lineTo(6f, 12f)
                moveTo(12f, 20f)
                lineTo(12f, 6f)
                moveTo(18f, 20f)
                lineTo(18f, 14f)
                moveTo(3f, 20f)
                lineTo(21f, 20f)
            }
        }.build()

    val SettingsGear: ImageVector
        get() = ImageVector.Builder(
            name = "SettingsGear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF757575)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, true, true, 12.01f, 15f)
                moveTo(19.4f, 15f)
                lineTo(20.8f, 13.6f)
                lineTo(18.8f, 10.2f)
                lineTo(16.9f, 10.9f)
                lineTo(15f, 9f)
                lineTo(15.7f, 7.1f)
                lineTo(12.3f, 5.1f)
                lineTo(11f, 6.8f)
            }
        }.build()

    // --- Settings UI Icons ---
    val Globe: ImageVector
        get() = ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, false, 12f, 22f)
                arcTo(10f, 10f, 0f, true, false, 12f, 2f)
                close()
                moveTo(2f, 12f)
                lineTo(22f, 12f)
                moveTo(12f, 2f)
                curveTo(14.5f, 6.5f, 14.5f, 17.5f, 12f, 22f)
                curveTo(9.5f, 17.5f, 9.5f, 6.5f, 12f, 2f)
                close()
            }
        }.build()

    val Moon: ImageVector
        get() = ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, true, true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, true, false, 21f, 12.79f)
                close()
            }
        }.build()

    val Login: ImageVector
        get() = ImageVector.Builder(
            name = "Login",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 3f)
                lineTo(19f, 3f)
                arcTo(2f, 2f, 0f, false, true, 21f, 5f)
                lineTo(21f, 19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                lineTo(15f, 21f)
                moveTo(10f, 17f)
                lineTo(15f, 12f)
                lineTo(10f, 7f)
                moveTo(15f, 12f)
                lineTo(3f, 12f)
            }
        }.build()

    val Info: ImageVector
        get() = ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, false, 12f, 22f)
                arcTo(10f, 10f, 0f, true, false, 12f, 2f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 16f)
                moveTo(12f, 8f)
                arcTo(1f, 1f, 0f, true, true, 12.01f, 8f)
            }
        }.build()

    val Shield: ImageVector
        get() = ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                lineTo(20f, 18f)
                lineTo(20f, 5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                lineTo(4f, 18f)
                close()
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()

    val SystemMonitor: ImageVector
        get() = ImageVector.Builder(
            name = "SystemMonitor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 6f)
                lineTo(22f, 6f)
                lineTo(22f, 18f)
                lineTo(2f, 18f)
                close()
                moveTo(8f, 22f)
                lineTo(16f, 22f)
                moveTo(12f, 18f)
                lineTo(12f, 22f)
            }
        }.build()
}
