package com.daftar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.daftar.app.ui.theme.CrimsonError
import com.daftar.app.ui.theme.EmeraldProfit
import com.daftar.app.ui.theme.GoldPrimary
import com.daftar.app.ui.theme.NavyClassic
import java.math.BigDecimal

data class BarChartDatum(
    val label: String,
    val primaryAmount: BigDecimal,
    val secondaryAmount: BigDecimal = BigDecimal.ZERO,
)

/**
 * Pure Canvas Financial Chart comparing Revenue vs Expenses or Cash Inflows vs Outflows.
 */
@Composable
fun FinancialComparisonChart(
    title: String,
    primaryLabel: String,
    secondaryLabel: String,
    data: List<BarChartDatum>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("● $primaryLabel", style = MaterialTheme.typography.labelMedium, color = EmeraldProfit)
                Text("● $secondaryLabel", style = MaterialTheme.typography.labelMedium, color = CrimsonError)
            }
            Spacer(Modifier.height(12.dp))
            val maxVal = data.maxOfOrNull { maxOf(it.primaryAmount, it.secondaryAmount) }
                ?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val count = data.size.coerceAtLeast(1)
                val groupWidth = size.width / count
                val barW = (groupWidth * 0.28f).coerceAtLeast(8f)
                val bottomY = size.height - 8f

                // Baseline
                drawLine(
                    color = GoldPrimary.copy(alpha = 0.4f),
                    start = Offset(0f, bottomY),
                    end = Offset(size.width, bottomY),
                    strokeWidth = 2f,
                )

                data.forEachIndexed { idx, d ->
                    val centerX = groupWidth * idx + groupWidth / 2f
                    val h1Ratio = d.primaryAmount.toFloat() / maxVal.toFloat()
                    val h2Ratio = d.secondaryAmount.toFloat() / maxVal.toFloat()
                    val h1 = (bottomY * 0.88f * h1Ratio).coerceAtLeast(4f)
                    val h2 = (bottomY * 0.88f * h2Ratio).coerceAtLeast(4f)

                    drawRoundRect(
                        color = EmeraldProfit,
                        topLeft = Offset(centerX - barW - 3f, bottomY - h1),
                        size = Size(barW, h1),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                    drawRoundRect(
                        color = CrimsonError,
                        topLeft = Offset(centerX + 3f, bottomY - h2),
                        size = Size(barW, h2),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                data.forEach { d ->
                    Text(
                        text = d.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerProportionBar(
    label: String,
    ratio: Float,
    color: Color = NavyClassic,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            drawRoundRect(
                color = GoldPrimary.copy(alpha = 0.18f),
                size = size,
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * ratio.coerceIn(0f, 1f), size.height),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
}
