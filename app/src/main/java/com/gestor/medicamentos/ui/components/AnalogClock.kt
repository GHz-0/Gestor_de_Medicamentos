package com.gestor.medicamentos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/** Reloj analógico pequeño, solo estético. */
@Composable
fun AnalogClock(now: Long, color: Color, modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        drawCircle(Color.White, r)
        drawCircle(color, r - 1.5f, style = Stroke(3f))
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val min = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        val hour = cal.get(Calendar.HOUR) + min / 60f
        fun hand(deg: Float, len: Float, w: Float) {
            val a = Math.toRadians(deg - 90.0)
            drawLine(color, c, Offset(c.x + (cos(a) * len).toFloat(), c.y + (sin(a) * len).toFloat()), w, StrokeCap.Round)
        }
        hand(hour * 30f, r * 0.5f, 4f)
        hand(min * 6f, r * 0.75f, 3f)
    }
}
