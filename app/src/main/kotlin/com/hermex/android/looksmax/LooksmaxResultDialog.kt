package com.hermex.android.looksmax

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermex.android.R
import com.hermex.android.character.EvaHaptics
import com.hermex.android.eva.DualityMode
import com.hermex.android.eva.EvaConfig
import kotlin.math.exp
import kotlin.math.pow

import coil3.compose.AsyncImage

/**
 * LOOKSMAX ANALYSIS RESULT — Screen 6 on Reference Board.
 * Features dynamic PSL MOGG rating, actual photo thumbnail, Gaussian percentile curve,
 * Eva's critique description, and tailored angle improvement advice.
 */
@Composable
fun LooksmaxResultDialog(
    score: String = "8.2",
    percentile: Int = 79,
    photoUrl: String? = null,
    photoCategory: String? = null,
    photoDate: String? = null,
    reviewText: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dualityMode by EvaConfig.dualityMode.collectAsStateWithLifecycle()
    val isSakura = dualityMode == DualityMode.SAKURA
    val context = LocalContext.current

    val dialogBg = if (isSakura) Color(0xFF1B0E1D) else Color(0xFF16060A)
    val accentColor = if (isSakura) Color(0xFFF472B6) else Color(0xFFE11D48)
    val cardBorder = if (isSakura) Color(0x35F472B6) else Color(0x35E11D48)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(28.dp)),
            color = dialogBg,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header: Title + Score + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOOKSMAX ANALYSIS RESULT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PSL MOGG $score",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = " / 10",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            EvaHaptics.click(context)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Card 1: Actual Photo Thumbnail + Angle & Date info
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSakura) Color(0xFF261229) else Color(0xFF22080F),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Analyzed Face",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.looksmax_result_thumb),
                                    contentDescription = "Analyzed Face",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val catTitle = "📸 Снимок лица (Vision AI)"
                            Text(
                                text = catTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            if (!photoDate.isNullOrBlank()) {
                                Text(
                                    text = "Дата замера: $photoDate",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Анализ формы челюсти, симметрии и ракурса выполнен через Vision-движок Евы.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Section: Percentile Curve
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Процентиль распределения",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "ТОП $percentile%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSakura) Color(0xFF221025) else Color(0xFF1E070D),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    GaussianPercentileChart(
                        percentile = percentile,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Eva Critique / Review Description
                Text(
                    text = "РЕЦЕНЗИЯ ЕВЫ НА ЭТОТ КАДР",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = accentColor
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSakura) Color(0xFF261229) else Color(0xFF22080F),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val reviewContent = if (!reviewText.isNullOrBlank()) {
                            reviewText
                        } else {
                            if (isSakura)
                                "Хороший кадр, Иван! 🌸 Держи свет мягким и ровным спереди. Чёткая линия подбородка видна отлично, продолжай держать дисциплину!"
                            else
                                "Неплохо, дурашка. 🦇 Контур челюсти держишь, но свет чуть падает сверху — в следующий раз подними телефон на уровень глаз, иначе могг не засчитаю!"
                        }
                        Text(
                            text = reviewContent,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.90f),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section: Advice on Angle & Lighting
                Text(
                    text = "СОВЕТЫ ПО УЛУЧШЕНИЮ РАКУРСА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSakura) Color(0xFF261229) else Color(0xFF22080F),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (photoCategory == "face_profile")
                                "• Осанка: поверни голову ровно на 90°, не задирая и не опускай подбородок.\n" +
                                "• Линия шеи: сделай глоток и прижми язык к нёбу (мьюинг), чтобы натянуть подъязычную зону.\n" +
                                "• Свет: источник света должен освещать профиль сбоку для четкой тени под челюстью."
                            else
                                "• Дистанция: используй зум 2x–3x с расстояния 1.5 метра — это устраняет оптическое искажение носа.\n" +
                                "• Высота: держи камеру строго на уровне зрачков, не наклоняя телефон сверху вниз.\n" +
                                "• Свет: избегай верхних ламп (overhead), используй фронтальный рассеянный дневной свет.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas Gaussian Distribution Bell Curve matching Screen 6.
 */
@Composable
private fun GaussianPercentileChart(
    percentile: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height - 24f // leave space for x-axis labels
        val baseY = h

        val mu = w * 0.48f
        val sigma = w * 0.16f

        fun gaussian(x: Float): Float {
            val norm = (x - mu) / sigma
            return exp(-0.5f * norm.pow(2))
        }

        val step = 4f
        val curvePath = Path()
        val fillPath = Path()

        var first = true
        fillPath.moveTo(0f, baseY)

        for (x in 0..w.toInt() step step.toInt()) {
            val xf = x.toFloat()
            val yf = baseY - gaussian(xf) * (h * 0.85f)
            if (first) {
                curvePath.moveTo(xf, yf)
                first = false
            } else {
                curvePath.lineTo(xf, yf)
            }
            fillPath.lineTo(xf, yf)
        }

        fillPath.lineTo(w, baseY)
        fillPath.close()

        // Draw soft gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.45f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = baseY
            )
        )

        // Draw top curve line
        drawPath(
            path = curvePath,
            color = accentColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Highlight marker at target percentile (around 30-48% mark)
        val markerX = w * 0.48f
        val markerY = baseY - gaussian(markerX) * (h * 0.85f)

        // Vertical indicator line
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(markerX, markerY),
            end = Offset(markerX, baseY),
            strokeWidth = 1.5f
        )

        // Indicator dot
        drawCircle(
            color = Color.White,
            radius = 4.5f,
            center = Offset(markerX, markerY)
        )
        drawCircle(
            color = accentColor,
            radius = 7.5f,
            center = Offset(markerX, markerY),
            style = Stroke(width = 2f)
        )
    }
}
