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

    val HomeFilled: ImageVector
        get() = ImageVector.Builder(
            name = "HomeFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2.5f)
                lineTo(1.5f, 11f)
                lineTo(3.2f, 13f)
                lineTo(4f, 12.3f)
                lineTo(4f, 21f)
                lineTo(10f, 21f)
                lineTo(10f, 14f)
                lineTo(14f, 14f)
                lineTo(14f, 21f)
                lineTo(20f, 21f)
                lineTo(20f, 12.3f)
                lineTo(20.8f, 13f)
                lineTo(22.5f, 11f)
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
                lineTo(6f, 15f)
                moveTo(10f, 20f)
                lineTo(10f, 10f)
                moveTo(14f, 20f)
                lineTo(14f, 5f)
                moveTo(18f, 20f)
                lineTo(18f, 13f)
                moveTo(22f, 20f)
                lineTo(22f, 17f)
                moveTo(3f, 20f)
                lineTo(23f, 20f)
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
                moveTo(12f, 2.5f)
                lineTo(14f, 2.8f)
                lineTo(14.6f, 5f)
                lineTo(16.2f, 5.7f)
                lineTo(18.4f, 4.6f)
                lineTo(19.8f, 6.1f)
                lineTo(18.7f, 8.2f)
                lineTo(19.4f, 9.8f)
                lineTo(21.6f, 10.4f)
                lineTo(21.6f, 13.6f)
                lineTo(19.4f, 14.2f)
                lineTo(18.7f, 15.8f)
                lineTo(19.8f, 17.9f)
                lineTo(18.4f, 19.4f)
                lineTo(16.2f, 18.3f)
                lineTo(14.6f, 19f)
                lineTo(14f, 21.2f)
                lineTo(10f, 21.2f)
                lineTo(9.4f, 19f)
                lineTo(7.8f, 18.3f)
                lineTo(5.6f, 19.4f)
                lineTo(4.2f, 17.9f)
                lineTo(5.3f, 15.8f)
                lineTo(4.6f, 14.2f)
                lineTo(2.4f, 13.6f)
                lineTo(2.4f, 10.4f)
                lineTo(4.6f, 9.8f)
                lineTo(5.3f, 8.2f)
                lineTo(4.2f, 6.1f)
                lineTo(5.6f, 4.6f)
                lineTo(7.8f, 5.7f)
                lineTo(9.4f, 5f)
                lineTo(10f, 2.8f)
                close()
                moveTo(15f, 12f)
                curveTo(15f, 13.7f, 13.7f, 15f, 12f, 15f)
                curveTo(10.3f, 15f, 9f, 13.7f, 9f, 12f)
                curveTo(9f, 10.3f, 10.3f, 9f, 12f, 9f)
                curveTo(13.7f, 9f, 15f, 10.3f, 15f, 12f)
                close()
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

    val Palette: ImageVector
        get() = ImageVector.Builder(
            name = "Palette",
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
                moveTo(12f, 3f)
                curveTo(6.9f, 3f, 3f, 6.8f, 3f, 11.5f)
                curveTo(3f, 16.2f, 6.8f, 20f, 11.5f, 20f)
                lineTo(13f, 20f)
                curveTo(14.3f, 20f, 15f, 18.4f, 14.2f, 17.4f)
                curveTo(13.5f, 16.5f, 14f, 15f, 15.2f, 15f)
                lineTo(17f, 15f)
                curveTo(19.2f, 15f, 21f, 13.2f, 21f, 11f)
                curveTo(21f, 6.6f, 17f, 3f, 12f, 3f)
                close()
            }
            path(fill = SolidColor(Color(0xFF00796B))) {
                moveTo(8f, 10f)
                arcTo(1f, 1f, 0f, true, true, 8.01f, 10f)
                close()
                moveTo(12f, 7.5f)
                arcTo(1f, 1f, 0f, true, true, 12.01f, 7.5f)
                close()
                moveTo(16f, 10f)
                arcTo(1f, 1f, 0f, true, true, 16.01f, 10f)
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

    val Clock: ImageVector
        get() = ImageVector.Builder(
            name = "Clock",
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
                arcTo(10f, 10f, 0f, true, false, 12f, 2f)
                arcTo(10f, 10f, 0f, true, false, 12f, 22f)
                close()
                moveTo(12f, 6f)
                lineTo(12f, 12f)
                lineTo(16f, 14f)
            }
        }.build()

    val Inventory: ImageVector
        get() = ImageVector.Builder(
            name = "Inventory",
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
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(22f, 8f)
                lineTo(22f, 20f)
                lineTo(2f, 20f)
                lineTo(2f, 8f)
                close()
                moveTo(2f, 8f)
                lineTo(22f, 8f)
                moveTo(9f, 12f)
                lineTo(15f, 12f)
            }
        }.build()

    val ContentCopy: ImageVector
        get() = ImageVector.Builder(
            name = "ContentCopy",
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
                moveTo(8f, 8f)
                lineTo(20f, 8f)
                lineTo(20f, 21f)
                lineTo(8f, 21f)
                close()
                moveTo(16f, 8f)
                lineTo(16f, 4f)
                lineTo(4f, 4f)
                lineTo(4f, 17f)
                lineTo(8f, 17f)
            }
        }.build()

    val Delete: ImageVector
        get() = ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFFC62828)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(10f, 11f)
                lineTo(10f, 17f)
                moveTo(14f, 11f)
                lineTo(14f, 17f)
                moveTo(5f, 7f)
                lineTo(6f, 21f)
                lineTo(18f, 21f)
                lineTo(19f, 7f)
                moveTo(9f, 7f)
                lineTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 7f)
            }
        }.build()

    val ChevronLeft: ImageVector
        get() = ImageVector.Builder(
            name = "ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF757575)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 18f)
                lineTo(9f, 12f)
                lineTo(15f, 6f)
            }
        }.build()

    val ChevronRight: ImageVector
        get() = ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF00796B)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }.build()

    val Menu: ImageVector
        get() = ImageVector.Builder(
            name = "Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 18f)
                lineTo(20f, 18f)
            }
        }.build()

    val Block: ImageVector
        get() = ImageVector.Builder(
            name = "Block",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, false, 12f, 22f)
                arcTo(10f, 10f, 0f, true, false, 12f, 2f)
                close()
                moveTo(5f, 5f)
                lineTo(19f, 19f)
            }
        }.build()

    val Edit: ImageVector
        get() = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0E6C7D)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20f)
                lineTo(21f, 20f)
                moveTo(16.5f, 3.5f)
                curveTo(17.3f, 2.7f, 18.7f, 2.7f, 19.5f, 3.5f)
                curveTo(20.3f, 4.3f, 20.3f, 5.7f, 19.5f, 6.5f)
                lineTo(8f, 18f)
                lineTo(3f, 20f)
                lineTo(5f, 15f)
                close()
            }
        }.build()

    val Smartphone: ImageVector
        get() = ImageVector.Builder(
            name = "Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF173451)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 2f)
                lineTo(17f, 2f)
                arcTo(2f, 2f, 0f, false, true, 19f, 4f)
                lineTo(19f, 20f)
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                lineTo(7f, 22f)
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                lineTo(5f, 4f)
                arcTo(2f, 2f, 0f, false, true, 7f, 2f)
                close()
                moveTo(11f, 18f)
                lineTo(13f, 18f)
            }
        }.build()

    val Facebook: ImageVector
        get() = ImageVector.Builder(
            name = "Facebook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(13.8f, 21f)
                lineTo(13.8f, 13.2f)
                lineTo(16.5f, 13.2f)
                lineTo(16.9f, 10.1f)
                lineTo(13.8f, 10.1f)
                lineTo(13.8f, 8.2f)
                curveTo(13.8f, 7.3f, 14.1f, 6.7f, 15.4f, 6.7f)
                lineTo(17f, 6.7f)
                lineTo(17f, 3.9f)
                curveTo(16.2f, 3.8f, 15.3f, 3.7f, 14.2f, 3.7f)
                curveTo(11.2f, 3.7f, 9.5f, 5.5f, 9.5f, 8.4f)
                lineTo(9.5f, 10.1f)
                lineTo(7f, 10.1f)
                lineTo(7f, 13.2f)
                lineTo(9.5f, 13.2f)
                lineTo(9.5f, 21f)
                close()
            }
        }.build()

    val Email: ImageVector
        get() = ImageVector.Builder(
            name = "Email",
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
                moveTo(4f, 5.5f)
                lineTo(20f, 5.5f)
                curveTo(21.1f, 5.5f, 22f, 6.4f, 22f, 7.5f)
                lineTo(22f, 16.5f)
                curveTo(22f, 17.6f, 21.1f, 18.5f, 20f, 18.5f)
                lineTo(4f, 18.5f)
                curveTo(2.9f, 18.5f, 2f, 17.6f, 2f, 16.5f)
                lineTo(2f, 7.5f)
                curveTo(2f, 6.4f, 2.9f, 5.5f, 4f, 5.5f)
                close()
                moveTo(3f, 7f)
                lineTo(12f, 13.5f)
                lineTo(21f, 7f)
            }
        }.build()

    val WhatsApp: ImageVector
        get() = ImageVector.Builder(
            name = "WhatsApp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 11.7f)
                curveTo(20.5f, 16.4f, 16.7f, 20.2f, 12f, 20.2f)
                curveTo(10.4f, 20.2f, 8.9f, 19.8f, 7.6f, 19f)
                lineTo(3.5f, 20.3f)
                lineTo(4.8f, 16.4f)
                curveTo(4f, 15.1f, 3.5f, 13.5f, 3.5f, 11.8f)
                curveTo(3.5f, 7.1f, 7.3f, 3.3f, 12f, 3.3f)
                curveTo(16.7f, 3.3f, 20.5f, 7.1f, 20.5f, 11.7f)
                close()
                moveTo(8.2f, 7.6f)
                curveTo(7.8f, 7.6f, 7.4f, 8.1f, 7.4f, 8.6f)
                curveTo(7.4f, 11.5f, 10.1f, 15.3f, 13.8f, 16.3f)
                curveTo(14.9f, 16.6f, 16.1f, 15.8f, 16.4f, 14.8f)
                lineTo(14.2f, 13.7f)
                lineTo(12.9f, 15f)
                curveTo(11.4f, 14.4f, 9.7f, 12.8f, 9.1f, 11.3f)
                lineTo(10.3f, 10f)
                lineTo(9.3f, 7.6f)
                close()
            }
        }.build()
}
