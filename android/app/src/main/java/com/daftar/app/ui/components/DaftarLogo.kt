package com.daftar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daftar.app.ui.theme.GoldLight
import com.daftar.app.ui.theme.GoldPrimary
import com.daftar.app.ui.theme.NavyClassic
import com.daftar.app.ui.theme.NavyDeep

enum class DaftarLogoVariant {
    DAY,
    NIGHT,
    PRINT_MONOCHROME,
}

/**
 * Original Geometric DAFTAR Ledger Emblem with Day, Night, and Print variants.
 */
@Composable
fun DaftarGeometricLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    variant: DaftarLogoVariant = DaftarLogoVariant.DAY,
    showWordmark: Boolean = false,
) {
    val bgColor = when (variant) {
        DaftarLogoVariant.DAY -> NavyClassic
        DaftarLogoVariant.NIGHT -> NavyDeep
        DaftarLogoVariant.PRINT_MONOCHROME -> Color.Transparent
    }
    val borderColor = when (variant) {
        DaftarLogoVariant.DAY -> GoldPrimary
        DaftarLogoVariant.NIGHT -> GoldLight
        DaftarLogoVariant.PRINT_MONOCHROME -> Color.Black
    }
    val pageStroke = when (variant) {
        DaftarLogoVariant.DAY -> Color.White
        DaftarLogoVariant.NIGHT -> Color(0xFFF1F5F9)
        DaftarLogoVariant.PRINT_MONOCHROME -> Color.Black
    }
    val goldAccent = when (variant) {
        DaftarLogoVariant.DAY -> GoldPrimary
        DaftarLogoVariant.NIGHT -> GoldLight
        DaftarLogoVariant.PRINT_MONOCHROME -> Color.Black
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val pad = w * 0.08f
            val corner = w * 0.22f

            if (bgColor != Color.Transparent) {
                drawRoundRect(
                    color = bgColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(pad * 0.4f, pad * 0.4f),
                size = Size(w - pad * 0.8f, h - pad * 0.8f),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f),
                style = Stroke(width = w * 0.045f),
            )

            // Open double-entry ledger frame
            val leftPage = Path().apply {
                moveTo(w * 0.50f, h * 0.30f)
                lineTo(w * 0.23f, h * 0.26f)
                lineTo(w * 0.23f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.76f)
                close()
            }
            val rightPage = Path().apply {
                moveTo(w * 0.50f, h * 0.30f)
                lineTo(w * 0.77f, h * 0.26f)
                lineTo(w * 0.77f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.76f)
                close()
            }
            drawPath(leftPage, color = pageStroke, style = Stroke(width = w * 0.042f, join = StrokeJoin.Round))
            drawPath(rightPage, color = goldAccent, style = Stroke(width = w * 0.042f, join = StrokeJoin.Round))

            // Ledger spine + horizontal balance bars
            drawLine(
                color = goldAccent,
                start = Offset(w * 0.50f, h * 0.28f),
                end = Offset(w * 0.50f, h * 0.79f),
                strokeWidth = w * 0.045f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = pageStroke.copy(alpha = 0.8f),
                start = Offset(w * 0.30f, h * 0.43f),
                end = Offset(w * 0.43f, h * 0.44f),
                strokeWidth = w * 0.032f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = pageStroke.copy(alpha = 0.8f),
                start = Offset(w * 0.30f, h * 0.54f),
                end = Offset(w * 0.43f, h * 0.55f),
                strokeWidth = w * 0.032f,
                cap = StrokeCap.Round,
            )

            // Verified accounting checkmark on right folio
            val checkPath = Path().apply {
                moveTo(w * 0.57f, h * 0.50f)
                lineTo(w * 0.63f, h * 0.57f)
                lineTo(w * 0.72f, h * 0.42f)
            }
            drawPath(
                path = checkPath,
                color = goldAccent,
                style = Stroke(width = w * 0.048f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        if (showWordmark) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "دفتر | DAFTAR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (variant == DaftarLogoVariant.NIGHT) GoldLight else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
