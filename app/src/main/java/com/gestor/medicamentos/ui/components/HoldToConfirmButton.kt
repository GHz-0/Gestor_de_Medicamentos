package com.gestor.medicamentos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gestor.medicamentos.ui.theme.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Botón que solo confirma si se mantiene presionado [seconds] segundos (barra de progreso). */
@Composable
fun HoldToConfirmButton(text: String, seconds: Int = 4, onConfirm: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Red.copy(alpha = 0.2f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        val t0 = System.currentTimeMillis()
                        while (true) {
                            progress = ((System.currentTimeMillis() - t0) / (seconds * 1000f)).coerceAtMost(1f)
                            if (progress >= 1f) { onConfirm(); break }
                            delay(16)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Red).align(Alignment.CenterStart))
        Text(text, color = if (progress > 0.5f) Color.White else Red)
    }
}
