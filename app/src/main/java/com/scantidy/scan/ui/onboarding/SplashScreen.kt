package com.scantidy.scan.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scantidy.scan.R
import com.scantidy.scan.ui.theme.Primary
import kotlinx.coroutines.delay

/**
 * Logo 闪屏页
 * - 展示品牌 Logo 和标语
 * - 淡入动画后，回调 onFinished 跳转
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(1800L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(Primary.value)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo 图标
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600))
            ) {
                AppLogoIcon(modifier = Modifier.size(96.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 品牌名称
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 200)) +
                        slideInVertically(tween(500, delayMillis = 200)) { it / 2 }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 副标题
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 500)) +
                        slideInVertically(tween(500, delayMillis = 500)) { it / 2 }
            ) {
                Text(
                    text = stringResource(R.string.app_subtitle),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * App Logo 矢量绘制（白色 PDF 文档图标 + 四角扫描框）
 */
@Composable
private fun AppLogoIcon(modifier: Modifier = Modifier) {
    val white = Color.White
    val accent = Color(Primary.value)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val iconSize = w * 0.78f
        val halfIcon = iconSize / 2f
        val docLeft = cx - halfIcon * 0.6f
        val docTop = cy - halfIcon * 0.7f
        val docRight = cx + halfIcon * 0.6f
        val docBottom = cy + halfIcon * 0.7f
        val foldX = docRight - halfIcon * 0.28f
        val foldY = docTop + halfIcon * 0.28f

        // 文档主体（折角文档形状）
        val docPath = Path().apply {
            moveTo(docLeft, docTop)
            lineTo(foldX, docTop)
            lineTo(foldX, foldY)
            lineTo(docRight, foldY)
            lineTo(docRight, docBottom)
            lineTo(docLeft, docBottom)
            close()
        }
        drawPath(docPath, white)

        // 折角阴影
        val foldDark = Path().apply {
            moveTo(foldX, docTop)
            lineTo(foldX, foldY)
            lineTo(docRight, foldY)
            close()
        }
        drawPath(foldDark, Color(0xFFD0D0D0))

        // 折角边线
        drawLine(Color(0xFFA0A0A0), Offset(foldX, docTop), Offset(foldX, foldY), 1.5f)
        drawLine(Color(0xFFA0A0A0), Offset(foldX, foldY), Offset(docRight, foldY), 1.5f)

        // PDF 文字（简化为 P / D / F 三个色条）
        val textLeft = docLeft + iconSize * 0.08f
        val textTop = docTop + iconSize * 0.55f
        val barW = iconSize * 0.045f
        val barH = iconSize * 0.22f
        val barGap = iconSize * 0.04f

        // P
        drawRect(accent, Offset(textLeft, textTop), Size(barW, barH))
        drawRect(accent, Offset(textLeft, textTop), Size(barW * 2.3f, barW))
        drawRect(accent, Offset(textLeft + barW * 1.3f, textTop + barW), Size(barW, barH * 0.45f))
        drawRect(accent, Offset(textLeft, textTop + barH * 0.55f), Size(barW, barH * 0.45f))

        // D
        val dx = textLeft + barW + barGap + barW * 1.8f
        drawRect(accent, Offset(dx, textTop), Size(barW, barH))
        val dCorner = barH * 0.35f
        val dHalf = barH * 0.65f
        drawArc(
            accent,
            topLeft = Offset(dx - dHalf, textTop + dCorner),
            size = Size(barW + dHalf * 2f, barH - dCorner * 2f),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = barW, cap = StrokeCap.Butt)
        )

        // F
        val fx = dx + barW + barGap * 1.8f
        drawRect(accent, Offset(fx, textTop), Size(barW, barH))
        drawRect(accent, Offset(fx, textTop), Size(barW * 2.2f, barW))
        drawRect(accent, Offset(fx, textTop + barH * 0.5f - barW * 0.5f), Size(barW * 1.6f, barW))

        // 扫描框四角
        val cornerLen = iconSize * 0.12f
        val frameInset = iconSize * 0.06f
        val frameL = cx - halfIcon - frameInset
        val frameT = cy - halfIcon - frameInset
        val frameR = cx + halfIcon + frameInset
        val frameB = cy + halfIcon + frameInset
        val sw = iconSize * 0.025f

        listOf(
            Offset(frameL, frameT + cornerLen) to Offset(frameL, frameT),
            Offset(frameL, frameT) to Offset(frameL + cornerLen, frameT),
            Offset(frameR - cornerLen, frameT) to Offset(frameR, frameT),
            Offset(frameR, frameT) to Offset(frameR, frameT + cornerLen),
            Offset(frameR, frameB - cornerLen) to Offset(frameR, frameB),
            Offset(frameR, frameB) to Offset(frameR - cornerLen, frameB),
            Offset(frameL + cornerLen, frameB) to Offset(frameL, frameB),
            Offset(frameL, frameB) to Offset(frameL, frameB - cornerLen)
        ).forEach { (start, end) ->
            drawLine(white, start, end, sw, cap = StrokeCap.Round)
        }
    }
}
